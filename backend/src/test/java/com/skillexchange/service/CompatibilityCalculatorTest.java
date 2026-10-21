package com.skillexchange.service;

import com.skillexchange.entity.Skill;
import com.skillexchange.entity.User;
import com.skillexchange.entity.UserSkill;
import com.skillexchange.entity.enums.SkillLevel;
import com.skillexchange.entity.enums.SkillType;
import com.skillexchange.repository.AvailabilityRepository;
import com.skillexchange.repository.RatingRepository;
import com.skillexchange.repository.UserSkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompatibilityCalculatorTest {

    @Mock private UserSkillRepository userSkillRepository;
    @Mock private AvailabilityRepository availabilityRepository;
    @Mock private RatingRepository ratingRepository;

    @InjectMocks
    private CompatibilityCalculator calculator;

    private Skill java;
    private Skill uiux;

    @BeforeEach
    void setUp() {
        java = Skill.builder().id(1L).name("Java").active(true).build();
        uiux = Skill.builder().id(2L).name("UI/UX Design").active(true).build();
    }

    private UserSkill us(Skill skill, SkillType type, SkillLevel level) {
        return UserSkill.builder().skill(skill).skillType(type).level(level).build();
    }

    @Test
    void perfectReciprocalMatchScoresHigh() {
        // I want to learn Java, they teach Java (EXPERT vs my target BEGINNER = ideal)
        List<UserSkill> myTeach = List.of();
        List<UserSkill> myLearn = List.of(us(java, SkillType.LEARN, SkillLevel.BEGINNER));
        List<UserSkill> theirTeach = List.of(us(java, SkillType.TEACH, SkillLevel.EXPERT));
        List<UserSkill> theirLearn = List.of();

        double score = calculator.calculateSkillCompatibility(myTeach, myLearn, theirTeach, theirLearn);
        assertTrue(score > 50.0, "Reciprocal skill match with strong level gap should score above 50, got " + score);
    }

    @Test
    void noOverlapScoresZeroOrNearZero() {
        List<UserSkill> myTeach = List.of(us(java, SkillType.TEACH, SkillLevel.EXPERT));
        List<UserSkill> myLearn = List.of(us(uiux, SkillType.LEARN, SkillLevel.BEGINNER));
        List<UserSkill> theirTeach = List.of(); // they teach nothing I want
        List<UserSkill> theirLearn = List.of(); // they want nothing I teach

        double score = calculator.calculateSkillCompatibility(myTeach, myLearn, theirTeach, theirLearn);
        assertEquals(0.0, score, 0.01);
    }

    @Test
    void emptyBothSidesReturnsZero() {
        double score = calculator.calculateSkillCompatibility(List.of(), List.of(), List.of(), List.of());
        assertEquals(0.0, score);
    }

    @Test
    void languageCompatibilityIsNeutralWhenDataMissing() {
        User me = User.builder().id(1L).languages(null).build();
        User candidate = User.builder().id(2L).languages("English").build();
        double score = calculator.calculateLanguageCompatibility(me, candidate);
        assertEquals(50.0, score);
    }

    @Test
    void languageCompatibilityFullOverlap() {
        User me = User.builder().id(1L).languages("English, Spanish").build();
        User candidate = User.builder().id(2L).languages("Spanish, French").build();
        double score = calculator.calculateLanguageCompatibility(me, candidate);
        assertEquals(50.0, score, 0.01); // 1 of 2 of my languages shared
    }

    @Test
    void reputationDefaultsToNeutralPositiveForNewUsers() {
        User candidate = User.builder().id(2L).build();
        when(ratingRepository.findAverageRatingForUser(2L)).thenReturn(null);
        double score = calculator.calculateReputationCompatibility(candidate);
        assertEquals(60.0, score);
    }

    @Test
    void reputationScalesWithAverageStars() {
        User candidate = User.builder().id(2L).build();
        when(ratingRepository.findAverageRatingForUser(2L)).thenReturn(5.0);
        assertEquals(100.0, calculator.calculateReputationCompatibility(candidate), 0.01);

        when(ratingRepository.findAverageRatingForUser(2L)).thenReturn(2.5);
        assertEquals(50.0, calculator.calculateReputationCompatibility(candidate), 0.01);
    }
}
