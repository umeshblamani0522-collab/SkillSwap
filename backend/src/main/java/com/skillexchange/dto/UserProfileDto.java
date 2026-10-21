package com.skillexchange.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserProfileDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String bio;
    private String location;
    private String profilePictureUrl;
    private String languages;
    private String communicationLanguage;
    private Integer experienceYears;
    private Double averageRating;
    private Long completedExchanges;
    private BigDecimal skillCredits;
    private Long reputationScore;
    private boolean suspended;
    private LocalDateTime createdAt;
    private List<UserSkillDto> teachSkills;
    private List<UserSkillDto> learnSkills;
}
