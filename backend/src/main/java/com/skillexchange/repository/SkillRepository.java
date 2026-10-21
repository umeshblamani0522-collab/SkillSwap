package com.skillexchange.repository;

import com.skillexchange.entity.Skill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    Optional<Skill> findByNameIgnoreCase(String name);
    Page<Skill> findByActiveTrueAndNameContainingIgnoreCase(String name, Pageable pageable);
    List<Skill> findByActiveTrue();
}
