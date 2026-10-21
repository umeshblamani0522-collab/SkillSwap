package com.skillexchange.dto.auth;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private String token;
    private Long userId;
    private String username;
    private String email;
    private List<String> roles;
    private String message;
}
