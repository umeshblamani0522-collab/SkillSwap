package com.skillexchange.service;

import com.skillexchange.dto.UpdateProfileRequest;
import com.skillexchange.dto.UserProfileDto;
import com.skillexchange.entity.User;
import com.skillexchange.entity.UserSkill;
import com.skillexchange.entity.enums.SkillType;
import com.skillexchange.exception.ResourceNotFoundException;
import com.skillexchange.mapper.EntityMapper;
import com.skillexchange.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;
    private final CreditAccountRepository creditAccountRepository;
    private final RatingRepository ratingRepository;
    private final SessionRepository sessionRepository;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return buildProfileDto(user, true);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getPublicProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return buildProfileDto(user, false);
    }

    @Transactional
    public UserProfileDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getLocation() != null) user.setLocation(request.getLocation());
        if (request.getProfilePictureUrl() != null) user.setProfilePictureUrl(request.getProfilePictureUrl());
        if (request.getLanguages() != null) user.setLanguages(request.getLanguages());
        if (request.getCommunicationLanguage() != null) user.setCommunicationLanguage(request.getCommunicationLanguage());
        if (request.getExperienceYears() != null) user.setExperienceYears(request.getExperienceYears());
        userRepository.save(user);
        return buildProfileDto(user, true);
    }

    @Transactional(readOnly = true)
    public Page<UserProfileDto> search(String query, Pageable pageable) {
        return userRepository.search(query == null ? "" : query, pageable).map(u -> buildProfileDto(u, false));
    }

    private UserProfileDto buildProfileDto(User user, boolean includePrivate) {
        List<UserSkill> teach = userSkillRepository.findByUserAndSkillType(user, SkillType.TEACH);
        List<UserSkill> learn = userSkillRepository.findByUserAndSkillType(user, SkillType.LEARN);

        BigDecimal balance = creditAccountRepository.findByUserId(user.getId())
                .map(a -> a.getBalance()).orElse(BigDecimal.ZERO);
        Double avgRating = ratingRepository.findAverageRatingForUser(user.getId());
        long completed = sessionRepository.findByTeacherIdOrLearnerIdOrderByCreatedAtDesc(user.getId(), user.getId()).stream()
                .filter(s -> s.getStatus() == com.skillexchange.entity.enums.SessionStatus.COMPLETED)
                .count();
        long reputationScore = Math.round((avgRating == null ? 0 : avgRating) * 20 + completed * 2);

        return UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(includePrivate ? user.getEmail() : null)
                .fullName(user.getFullName())
                .phone(includePrivate ? user.getPhone() : null)
                .bio(user.getBio())
                .location(user.getLocation())
                .profilePictureUrl(user.getProfilePictureUrl())
                .languages(user.getLanguages())
                .communicationLanguage(user.getCommunicationLanguage())
                .experienceYears(user.getExperienceYears())
                .averageRating(avgRating)
                .completedExchanges(completed)
                .skillCredits(includePrivate ? balance : null)
                .reputationScore(reputationScore)
                .suspended(includePrivate && user.isSuspended())
                .createdAt(user.getCreatedAt())
                .teachSkills(teach.stream().map(mapper::toDto).toList())
                .learnSkills(learn.stream().map(mapper::toDto).toList())
                .build();
    }
}
