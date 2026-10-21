package com.skillexchange.repository;

import com.skillexchange.entity.User;
import com.skillexchange.entity.UserSkill;
import com.skillexchange.entity.enums.SkillType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {
    List<UserSkill> findByUserAndSkillType(User user, SkillType skillType);

    List<UserSkill> findBySkillIdAndSkillType(Long skillId, SkillType skillType);

    List<UserSkill> findByUserId(Long userId);

    Optional<UserSkill> findByUserIdAndSkillIdAndSkillType(Long userId, Long skillId, SkillType skillType);

    boolean existsByUserIdAndSkillIdAndSkillType(Long userId, Long skillId, SkillType skillType);

    void deleteByUserId(Long userId);
}
