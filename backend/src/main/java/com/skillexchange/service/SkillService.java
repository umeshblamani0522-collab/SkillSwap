package com.skillexchange.service;

import com.skillexchange.dto.UserSkillDto;
import com.skillexchange.dto.skill.AddUserSkillRequest;
import com.skillexchange.dto.skill.SkillCategoryDto;
import com.skillexchange.dto.skill.SkillDto;
import com.skillexchange.entity.Skill;
import com.skillexchange.entity.SkillCategory;
import com.skillexchange.entity.User;
import com.skillexchange.entity.UserSkill;
import com.skillexchange.exception.DuplicateResourceException;
import com.skillexchange.exception.ResourceNotFoundException;
import com.skillexchange.mapper.EntityMapper;
import com.skillexchange.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;
    private final SkillCategoryRepository skillCategoryRepository;
    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public List<SkillCategoryDto> listCategories() {
        return skillCategoryRepository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Page<SkillDto> searchSkills(String query, Pageable pageable) {
        return skillRepository.findByActiveTrueAndNameContainingIgnoreCase(query == null ? "" : query, pageable)
                .map(mapper::toDto);
    }

    @Transactional
    public UserSkillDto addUserSkill(Long userId, AddUserSkillRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Skill skill = skillRepository.findById(request.getSkillId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found"));
        if (!skill.isActive()) {
            throw new com.skillexchange.exception.BusinessRuleException("This skill is no longer active");
        }

        userSkillRepository.findByUserIdAndSkillIdAndSkillType(userId, skill.getId(), request.getSkillType())
                .ifPresent(us -> { throw new DuplicateResourceException("This skill is already in your " +
                        request.getSkillType().name().toLowerCase() + " list"); });

        UserSkill userSkill = UserSkill.builder()
                .user(user).skill(skill)
                .skillType(request.getSkillType())
                .level(request.getLevel())
                .experienceDescription(request.getExperienceDescription())
                .build();
        return mapper.toDto(userSkillRepository.save(userSkill));
    }

    @Transactional
    public UserSkillDto updateUserSkill(Long userId, Long userSkillId, AddUserSkillRequest request) {
        UserSkill userSkill = userSkillRepository.findById(userSkillId)
                .orElseThrow(() -> new ResourceNotFoundException("User skill not found"));
        if (!userSkill.getUser().getId().equals(userId)) {
            throw new com.skillexchange.exception.AccessDeniedCustomException("You can only edit your own skills");
        }
        if (request.getLevel() != null) userSkill.setLevel(request.getLevel());
        if (request.getExperienceDescription() != null) userSkill.setExperienceDescription(request.getExperienceDescription());
        return mapper.toDto(userSkillRepository.save(userSkill));
    }

    @Transactional
    public void deleteUserSkill(Long userId, Long userSkillId) {
        UserSkill userSkill = userSkillRepository.findById(userSkillId)
                .orElseThrow(() -> new ResourceNotFoundException("User skill not found"));
        if (!userSkill.getUser().getId().equals(userId)) {
            throw new com.skillexchange.exception.AccessDeniedCustomException("You can only delete your own skills");
        }
        userSkillRepository.delete(userSkill);
    }

    @Transactional
    public SkillDto createSkill(String name, Long categoryId) {
        skillRepository.findByNameIgnoreCase(name).ifPresent(s -> {
            throw new DuplicateResourceException("Skill already exists");
        });
        SkillCategory category = categoryId != null
                ? skillCategoryRepository.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException("Category not found"))
                : null;
        Skill skill = Skill.builder().name(name).category(category).active(true).build();
        return mapper.toDto(skillRepository.save(skill));
    }

    @Transactional
    public SkillCategoryDto createCategory(String name, String description) {
        SkillCategory category = SkillCategory.builder().name(name).description(description).build();
        return mapper.toDto(skillCategoryRepository.save(category));
    }

    @Transactional
    public void setSkillActive(Long skillId, boolean active) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found"));
        skill.setActive(active);
        skillRepository.save(skill);
    }
}
