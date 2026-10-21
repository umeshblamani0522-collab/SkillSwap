package com.skillexchange.entity;

import com.skillexchange.entity.enums.SkillLevel;
import com.skillexchange.entity.enums.SkillType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_skills", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "skill_id", "skill_type"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserSkill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_type", nullable = false, length = 10)
    private SkillType skillType; // TEACH or LEARN

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", length = 20)
    private SkillLevel level;

    @Column(name = "experience_description", length = 1000)
    private String experienceDescription;
}
