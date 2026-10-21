package com.skillexchange.service;

import com.skillexchange.entity.PasswordResetToken;
import com.skillexchange.entity.User;
import com.skillexchange.exception.BusinessRuleException;
import com.skillexchange.repository.PasswordResetTokenRepository;
import com.skillexchange.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailService mailService;

    @Value("${app.password-reset.expiration-minutes:30}")
    private long expirationMinutes;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void requestReset(String email, String frontendOrigin) {
        tokenRepository.deleteExpiredOrUsed(LocalDateTime.now());
        userRepository.findByEmail(email.trim().toLowerCase()).filter(User::isEmailVerified).ifPresent(user -> {
            tokenRepository.deleteByUserId(user.getId());
            String rawToken = generateToken();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(hash(rawToken))
                    .expiresAt(LocalDateTime.now().plusMinutes(expirationMinutes))
                    .createdAt(LocalDateTime.now())
                    .build();
            tokenRepository.save(resetToken);
            String baseUrl = resolveFrontendUrl(frontendOrigin);
            String resetLink = baseUrl.replaceAll("/$", "") + "/reset-password?token=" + rawToken;
            mailService.sendResetEmail(user.getEmail(), user.getFullName(), resetLink, expirationMinutes);
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BusinessRuleException("Invalid or expired password reset link"));

        if (resetToken.getUsedAt() != null || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Invalid or expired password reset link");
        }

        User user = resetToken.getUser();
        if (!user.isEnabled() || user.isSuspended()) {
            throw new BusinessRuleException("This account cannot reset its password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        resetToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(resetToken);
        tokenRepository.deleteByUserId(user.getId());
    }

    private String resolveFrontendUrl(String requestedOrigin) {
        if (requestedOrigin == null || requestedOrigin.isBlank()) return frontendUrl;
        try {
            java.net.URI uri = java.net.URI.create(requestedOrigin.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            if (!"http".equalsIgnoreCase(scheme) || port != 5173 || host == null) return frontendUrl;
            String h = host.toLowerCase(java.util.Locale.ROOT);
            boolean localHost = h.equals("localhost") || h.equals("127.0.0.1");
            boolean private10 = h.matches("10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
            boolean private192 = h.matches("192\\.168\\.\\d{1,3}\\.\\d{1,3}");
            boolean private172 = h.matches("172\\.(1[6-9]|2\\d|3[0-1])\\.\\d{1,3}\\.\\d{1,3}");
            return (localHost || private10 || private192 || private172) ? requestedOrigin.trim() : frontendUrl;
        } catch (IllegalArgumentException ex) {
            return frontendUrl;
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash reset token", ex);
        }
    }
}
