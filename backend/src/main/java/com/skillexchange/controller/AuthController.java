package com.skillexchange.controller;

import com.skillexchange.dto.auth.AuthResponse;
import com.skillexchange.dto.auth.LoginRequest;
import com.skillexchange.dto.auth.ForgotPasswordRequest;
import com.skillexchange.dto.auth.ResetPasswordRequest;
import com.skillexchange.service.PasswordResetService;
import com.skillexchange.dto.auth.RegisterRequest;
import com.skillexchange.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final com.skillexchange.service.EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                     @RequestHeader(value = "Origin", required = false) String origin) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request, origin));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@RequestBody java.util.Map<String, String> body,
                                                     @RequestHeader(value = "Origin", required = false) String origin) {
        emailVerificationService.resend(body.getOrDefault("email", ""), origin);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        emailVerificationService.verify(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                  @RequestHeader(value = "Origin", required = false) String origin) {
        passwordResetService.requestReset(request.getEmail(), origin);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
