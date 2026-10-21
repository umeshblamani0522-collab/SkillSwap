package com.skillexchange.service;

import com.skillexchange.dto.match.MatchResultDto;
import com.skillexchange.entity.User;
import com.skillexchange.entity.UserSkill;
import com.skillexchange.entity.enums.SkillType;
import com.skillexchange.repository.UserRepository;
import com.skillexchange.repository.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates the skill-matching workflow: gathers candidates, delegates
 * per-dimension scoring to CompatibilityCalculator, applies the overall
 * weighting formula, and ranks results. Kept out of controllers so the
 * algorithm can be extended (e.g. new weights, ML re-ranking) independently.
 */
@Service
@RequiredArgsConstructor
public class MatchingService {

    // Weights are centralized here so the overall formula is easy to tune.
    private static final double W_SKILL = 0.50;
    private static final double W_AVAILABILITY = 0.20;
    private static final double W_LANGUAGE = 0.15;
    private static final double W_REPUTATION = 0.15;

    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;
    private final CompatibilityCalculator compatibilityCalculator;

    @Transactional(readOnly = true)
    public List<MatchResultDto> findMatchesForUser(Long userId, int limit) {
        User me = userRepository.findById(userId)
                .orElseThrow(() -> new com.skillexchange.exception.ResourceNotFoundException("User not found"));

        List<UserSkill> myTeach = userSkillRepository.findByUserAndSkillType(me, SkillType.TEACH);
        List<UserSkill> myLearn = userSkillRepository.findByUserAndSkillType(me, SkillType.LEARN);

        Set<Long> candidateIds = new HashSet<>();
        for (UserSkill wanted : myLearn) {
            if (!wanted.getSkill().isActive()) continue; // Business rule: inactive skills excluded from matching
            userSkillRepository.findBySkillIdAndSkillType(wanted.getSkill().getId(), SkillType.TEACH)
                    .forEach(us -> candidateIds.add(us.getUser().getId()));
        }
        candidateIds.remove(userId);

        List<MatchResult> results = new ArrayList<>();
        for (Long candidateId : candidateIds) {
            User candidate = userRepository.findById(candidateId).orElse(null);
            if (candidate == null || candidate.isSuspended()) continue;

            List<UserSkill> theirTeach = userSkillRepository.findByUserAndSkillType(candidate, SkillType.TEACH);
            List<UserSkill> theirLearn = userSkillRepository.findByUserAndSkillType(candidate, SkillType.LEARN);

            double skillScore = compatibilityCalculator.calculateSkillCompatibility(myTeach, myLearn, theirTeach, theirLearn);
            double availabilityScore = compatibilityCalculator.calculateAvailabilityCompatibility(me, candidate);
            double languageScore = compatibilityCalculator.calculateLanguageCompatibility(me, candidate);
            double reputationScore = compatibilityCalculator.calculateReputationCompatibility(candidate);

            double overall = (skillScore * W_SKILL) + (availabilityScore * W_AVAILABILITY)
                    + (languageScore * W_LANGUAGE) + (reputationScore * W_REPUTATION);

            List<String> theyCanTeachYouWant = intersectNames(myLearn, theirTeach);
            List<String> youCanTeachTheyWant = intersectNames(myTeach, theirLearn);

            results.add(MatchResult.builder()
                    .candidate(candidate)
                    .skillCompatibility(round(skillScore))
                    .availabilityCompatibility(round(availabilityScore))
                    .languageCompatibility(round(languageScore))
                    .reputationCompatibility(round(reputationScore))
                    .overallMatchPercentage(round(overall))
                    .theyCanTeachYouWant(theyCanTeachYouWant)
                    .youCanTeachTheyWant(youCanTeachTheyWant)
                    .build());
        }

        return results.stream()
                .sorted(Comparator.comparingDouble(MatchResult::getOverallMatchPercentage).reversed())
                .limit(limit)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MatchResultDto calculateMatchBetween(Long userId, Long candidateId) {
        User me = userRepository.findById(userId)
                .orElseThrow(() -> new com.skillexchange.exception.ResourceNotFoundException("User not found"));
        User candidate = userRepository.findById(candidateId)
                .orElseThrow(() -> new com.skillexchange.exception.ResourceNotFoundException("Candidate not found"));

        List<UserSkill> myTeach = userSkillRepository.findByUserAndSkillType(me, SkillType.TEACH);
        List<UserSkill> myLearn = userSkillRepository.findByUserAndSkillType(me, SkillType.LEARN);
        List<UserSkill> theirTeach = userSkillRepository.findByUserAndSkillType(candidate, SkillType.TEACH);
        List<UserSkill> theirLearn = userSkillRepository.findByUserAndSkillType(candidate, SkillType.LEARN);

        double skillScore = compatibilityCalculator.calculateSkillCompatibility(myTeach, myLearn, theirTeach, theirLearn);
        double availabilityScore = compatibilityCalculator.calculateAvailabilityCompatibility(me, candidate);
        double languageScore = compatibilityCalculator.calculateLanguageCompatibility(me, candidate);
        double reputationScore = compatibilityCalculator.calculateReputationCompatibility(candidate);
        double overall = (skillScore * W_SKILL) + (availabilityScore * W_AVAILABILITY)
                + (languageScore * W_LANGUAGE) + (reputationScore * W_REPUTATION);

        MatchResult result = MatchResult.builder()
                .candidate(candidate)
                .skillCompatibility(round(skillScore))
                .availabilityCompatibility(round(availabilityScore))
                .languageCompatibility(round(languageScore))
                .reputationCompatibility(round(reputationScore))
                .overallMatchPercentage(round(overall))
                .theyCanTeachYouWant(intersectNames(myLearn, theirTeach))
                .youCanTeachTheyWant(intersectNames(myTeach, theirLearn))
                .build();

        return toDto(result);
    }

    private List<String> intersectNames(List<UserSkill> wanted, List<UserSkill> offered) {
        Set<Long> wantedIds = wanted.stream().map(us -> us.getSkill().getId()).collect(Collectors.toSet());
        return offered.stream()
                .filter(us -> wantedIds.contains(us.getSkill().getId()))
                .map(us -> us.getSkill().getName())
                .distinct()
                .collect(Collectors.toList());
    }

    private double round(double v) { return Math.round(v * 100.0) / 100.0; }

    private MatchResultDto toDto(MatchResult r) {
        return MatchResultDto.builder()
                .userId(r.getCandidate().getId())
                .username(r.getCandidate().getUsername())
                .fullName(r.getCandidate().getFullName())
                .profilePictureUrl(r.getCandidate().getProfilePictureUrl())
                .overallMatchPercentage(r.getOverallMatchPercentage())
                .skillCompatibility(r.getSkillCompatibility())
                .availabilityCompatibility(r.getAvailabilityCompatibility())
                .languageCompatibility(r.getLanguageCompatibility())
                .reputationCompatibility(r.getReputationCompatibility())
                .theyCanTeachYouWant(r.getTheyCanTeachYouWant())
                .youCanTeachTheyWant(r.getYouCanTeachTheyWant())
                .videoCallEligible(r.getOverallMatchPercentage() >= 60.0
                        && !r.getTheyCanTeachYouWant().isEmpty()
                        && !r.getYouCanTeachTheyWant().isEmpty())
                .videoCallEligibilityMessage(r.getOverallMatchPercentage() >= 60.0
                        && !r.getTheyCanTeachYouWant().isEmpty()
                        && !r.getYouCanTeachTheyWant().isEmpty()
                        ? "Strong reciprocal match"
                        : "Video intro opens for strong reciprocal matches")
                .build();
    }
}
