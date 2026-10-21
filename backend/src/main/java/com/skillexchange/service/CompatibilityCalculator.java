package com.skillexchange.service;

import com.skillexchange.entity.Availability;
import com.skillexchange.entity.User;
import com.skillexchange.entity.UserSkill;
import com.skillexchange.entity.enums.SkillLevel;
import com.skillexchange.repository.AvailabilityRepository;
import com.skillexchange.repository.RatingRepository;
import com.skillexchange.entity.enums.SkillType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Pure calculation logic for individual compatibility dimensions between two users.
 * Kept separate from MatchingService so the scoring weights/formulas can be
 * unit-tested and evolved independently of persistence/orchestration concerns.
 */
@Component
@RequiredArgsConstructor
public class CompatibilityCalculator {

    private final AvailabilityRepository availabilityRepository;
    private final RatingRepository ratingRepository;

    /**
     * Skill compatibility: how well what candidate can TEACH overlaps with what
     * the current user wants to LEARN, and vice versa - the reciprocity that
     * makes an exchange possible in the first place. Level proximity also factors in.
     */
    public double calculateSkillCompatibility(List<UserSkill> myTeach, List<UserSkill> myLearn,
                                               List<UserSkill> theirTeach, List<UserSkill> theirLearn) {
        Set<Long> myLearnSkillIds = myLearn.stream().map(us -> us.getSkill().getId()).collect(Collectors.toSet());
        Set<Long> myTeachSkillIds = myTeach.stream().map(us -> us.getSkill().getId()).collect(Collectors.toSet());
        Set<Long> theirTeachSkillIds = theirTeach.stream().map(us -> us.getSkill().getId()).collect(Collectors.toSet());
        Set<Long> theirLearnSkillIds = theirLearn.stream().map(us -> us.getSkill().getId()).collect(Collectors.toSet());

        if (myLearnSkillIds.isEmpty() && myTeachSkillIds.isEmpty()) return 0.0;

        long theyTeachWhatIWant = myLearnSkillIds.stream().filter(theirTeachSkillIds::contains).count();
        long iTeachWhatTheyWant = myTeachSkillIds.stream().filter(theirLearnSkillIds::contains).count();

        double reciprocityScore = 0.0;
        int denom = Math.max(myLearnSkillIds.size(), 1) + Math.max(myTeachSkillIds.size(), 1);
        reciprocityScore = 100.0 * (theyTeachWhatIWant + iTeachWhatTheyWant) / denom;

        // Level-proximity bonus: closer skill levels on overlapping skills score higher
        double levelBonus = averageLevelProximity(myLearn, theirTeach);

        double score = (reciprocityScore * 0.75) + (levelBonus * 0.25);
        return clamp(score);
    }

    private double averageLevelProximity(List<UserSkill> myLearn, List<UserSkill> theirTeach) {
        Map<Long, SkillLevel> theirTeachLevels = theirTeach.stream()
                .collect(Collectors.toMap(us -> us.getSkill().getId(), UserSkill::getLevel, (a, b) -> a));

        List<Double> proximities = new ArrayList<>();
        for (UserSkill wanted : myLearn) {
            SkillLevel theirLevel = theirTeachLevels.get(wanted.getSkill().getId());
            if (theirLevel != null) {
                // A teacher one or more levels above the learner's target is ideal;
                // same level or below is still useful but less so.
                int diff = theirLevel.ordinal() - (wanted.getLevel() != null ? wanted.getLevel().ordinal() : 0);
                double proximity = diff >= 0 ? 100.0 : Math.max(0, 100.0 + diff * 25.0);
                proximities.add(proximity);
            }
        }
        if (proximities.isEmpty()) return 50.0; // neutral when no overlapping data
        return proximities.stream().mapToDouble(d -> d).average().orElse(50.0);
    }

    public double calculateAvailabilityCompatibility(User me, User candidate) {
        List<Availability> mine = availabilityRepository.findByUser(me);
        List<Availability> theirs = availabilityRepository.findByUser(candidate);
        if (mine.isEmpty() || theirs.isEmpty()) return 50.0; // neutral if unset

        long overlappingSlots = 0;
        for (Availability a : mine) {
            for (Availability b : theirs) {
                if (a.getDayOfWeek() == b.getDayOfWeek()
                        && a.getStartTime().isBefore(b.getEndTime())
                        && b.getStartTime().isBefore(a.getEndTime())) {
                    overlappingSlots++;
                }
            }
        }
        double ratio = (double) overlappingSlots / Math.max(mine.size(), 1);
        return clamp(ratio * 100.0);
    }

    public double calculateLanguageCompatibility(User me, User candidate) {
        if (isBlank(me.getLanguages()) || isBlank(candidate.getLanguages())) return 50.0;
        Set<String> mine = splitLanguages(me.getLanguages());
        Set<String> theirs = splitLanguages(candidate.getLanguages());
        if (mine.isEmpty() || theirs.isEmpty()) return 50.0;
        long shared = mine.stream().filter(theirs::contains).count();
        return clamp(100.0 * shared / mine.size());
    }

    public double calculateReputationCompatibility(User candidate) {
        Double avg = ratingRepository.findAverageRatingForUser(candidate.getId());
        if (avg == null) return 60.0; // neutral-positive default for new users
        return clamp((avg / 5.0) * 100.0);
    }

    private Set<String> splitLanguages(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim).map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private double clamp(double v) { return Math.max(0.0, Math.min(100.0, v)); }
}
