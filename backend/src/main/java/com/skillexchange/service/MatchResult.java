package com.skillexchange.service;

import com.skillexchange.entity.User;
import lombok.*;

import java.util.List;

/**
 * Immutable internal result of a compatibility computation between the current
 * user and a candidate, before it is converted to a client-facing DTO.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MatchResult {
    private User candidate;
    private double skillCompatibility;      // 0-100
    private double availabilityCompatibility; // 0-100
    private double languageCompatibility;     // 0-100
    private double reputationCompatibility;   // 0-100
    private double overallMatchPercentage;    // weighted 0-100
    private List<String> theyCanTeachYouWant;
    private List<String> youCanTeachTheyWant;
}
