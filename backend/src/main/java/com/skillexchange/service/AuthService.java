package com.skillexchange.service;

import com.skillexchange.dto.auth.AuthResponse;
import com.skillexchange.dto.auth.LoginRequest;
import com.skillexchange.dto.auth.RegisterRequest;
import com.skillexchange.entity.CreditAccount;
import com.skillexchange.entity.Role;
import com.skillexchange.entity.User;
import com.skillexchange.entity.enums.RoleName;
import com.skillexchange.exception.DuplicateResourceException;
import com.skillexchange.repository.CreditAccountRepository;
import com.skillexchange.repository.RoleRepository;
import com.skillexchange.repository.UserRepository;
import com.skillexchange.security.JwtUtil;
import com.skillexchange.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.skillexchange.repository.BlockedEmailRepository;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@org.springframework.transaction.annotation.Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final CreditAccountRepository creditAccountRepository;
        private final PasswordEncoder passwordEncoder;
        private final AuthenticationManager authenticationManager;
        private final JwtUtil jwtUtil;
        private final EmailVerificationService emailVerificationService;
        private final BlockedEmailRepository blockedEmailRepository;

        @Transactional
        public AuthResponse register(RegisterRequest request, String frontendOrigin) {
                String username = request.getUsername().trim();

                String email = request.getEmail()
                                .trim()
                                .toLowerCase(java.util.Locale.ROOT);

                // 1. Check whether the email is permanently blocked by an admin.
                if (blockedEmailRepository.existsByEmail(email)) {
                        throw new DuplicateResourceException(
                                        "This email address is permanently blocked from registering on SkillSwap");
                }

                // 2. Check whether the username is already taken.
                if (userRepository.findByUsername(username).isPresent()
                                || userRepository.findByUsername(
                                                username.toLowerCase(java.util.Locale.ROOT)).isPresent()) {
                        throw new DuplicateResourceException("Username is already taken");
                }

                // 3. Check whether the email is already registered.
                if (userRepository.findByEmail(email).isPresent()) {
                        throw new DuplicateResourceException("Email is already registered");
                }

                Role userRole = roleRepository.findByName(RoleName.USER)
                                .orElseThrow(() -> new IllegalStateException("USER role not seeded"));

                Set<Role> roles = new HashSet<>();
                roles.add(userRole);

                User user = User.builder()
                                .username(username)
                                .email(email)
                                .password(passwordEncoder.encode(request.getPassword()))
                                .fullName(request.getFullName())
                                .roles(roles)
                                .enabled(true)
                                .suspended(false)
                                .emailVerified(false)
                                .build();
                user = userRepository.save(user);

                CreditAccount account = CreditAccount.builder()
                                .user(user)
                                .balance(BigDecimal.valueOf(5)) // starter credits, welcome bonus
                                .totalEarned(BigDecimal.valueOf(5))
                                .totalSpent(BigDecimal.ZERO)
                                .build();
                creditAccountRepository.save(account);

                emailVerificationService.sendVerification(user, frontendOrigin);
                return AuthResponse.builder()
                                .message("Account created. Please verify your email address before signing in.")
                                .userId(user.getId())
                                .username(user.getUsername())
                                .email(user.getEmail())
                                .roles(List.of("USER"))
                                .build();
        }

        public AuthResponse login(LoginRequest request) {
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(),
                                                request.getPassword()));

                UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
                User user = userRepository.findById(principal.getId())
                                .orElseThrow(() -> new IllegalStateException("Authenticated user vanished"));
                if (!user.isEmailVerified()) {
                        throw new com.skillexchange.exception.EmailNotVerifiedException(user.getEmail());
                }

                List<String> roleNames = user.getRoles().stream().map(r -> r.getName().name()).toList();
                String token = jwtUtil.generateToken(user.getId(), user.getUsername(),
                                roleNames.stream().map(r -> "ROLE_" + r).toList());

                return AuthResponse.builder()
                                .token(token)
                                .userId(user.getId())
                                .username(user.getUsername())
                                .email(user.getEmail())
                                .roles(roleNames)
                                .build();
        }
}
