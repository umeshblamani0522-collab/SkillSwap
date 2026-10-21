package com.skillexchange.dto.admin;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminUserDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private boolean suspended;
    private boolean enabled;
    private List<String> roles;
    private LocalDateTime createdAt;
}
