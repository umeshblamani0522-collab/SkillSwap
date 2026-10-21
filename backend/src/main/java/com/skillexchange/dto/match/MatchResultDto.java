package com.skillexchange.dto.match;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MatchResultDto {
    private Long userId;
    private String username;
    private String fullName;
    private String profilePictureUrl;
    private Double overallMatchPercentage;
    private Double skillCompatibility;
    private Double availabilityCompatibility;
    private Double languageCompatibility;
    private Double reputationCompatibility;
    private List<String> theyCanTeachYouWant;
    private List<String> youCanTeachTheyWant;
    private boolean videoCallEligible;
    private String videoCallEligibilityMessage;
}
