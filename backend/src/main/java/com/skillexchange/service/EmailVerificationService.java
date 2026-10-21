package com.skillexchange.service;

import com.skillexchange.entity.EmailVerificationToken;
import com.skillexchange.entity.User;
import com.skillexchange.exception.BusinessRuleException;
import com.skillexchange.repository.EmailVerificationTokenRepository;
import com.skillexchange.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.net.URI;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailVerificationMailService mailService;
    @Value("${app.email-verification.expiration-minutes:30}") private long expirationMinutes;
    @Value("${app.frontend-url:http://localhost:5173}") private String frontendUrl;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public void sendVerification(User user, String requestedOrigin) {
    tokenRepository.deleteByUserId(user.getId());
    tokenRepository.flush();

    String raw = token();
        tokenRepository.save(EmailVerificationToken.builder().user(user).tokenHash(hash(raw))
                .expiresAt(LocalDateTime.now().plusMinutes(expirationMinutes)).createdAt(LocalDateTime.now()).build());
        String baseUrl = resolveFrontendUrl(requestedOrigin);
        String link = baseUrl.replaceAll("/$", "") + "/verify-email?token=" + raw;
        mailService.sendVerificationEmail(user.getEmail(), user.getFullName(), link, expirationMinutes);
    }

    @Transactional
    public void resend(String email, String requestedOrigin) {
        userRepository.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT)).ifPresent(user -> {
            if (!user.isEmailVerified() && user.isEnabled() && !user.isSuspended()) sendVerification(user, requestedOrigin);
        });
    }

    @Transactional
    public void verify(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw new BusinessRuleException("Invalid verification link");
        EmailVerificationToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BusinessRuleException("Invalid or expired verification link"));
        if (token.getVerifiedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new BusinessRuleException("Invalid or expired verification link");
        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        token.setVerifiedAt(LocalDateTime.now());
        tokenRepository.save(token);
    }

    private String resolveFrontendUrl(String requestedOrigin) {
        if (requestedOrigin == null || requestedOrigin.isBlank()) return frontendUrl;
        try {
            URI uri = URI.create(requestedOrigin.trim());
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

    private String token() { byte[] b = new byte[32]; random.nextBytes(b); return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }
    private String hash(String token) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder h = new StringBuilder(64); for (byte b : d) h.append(String.format("%02x", b)); return h.toString();
        } catch (Exception e) { throw new IllegalStateException("Unable to hash verification token", e); }
    }
}
