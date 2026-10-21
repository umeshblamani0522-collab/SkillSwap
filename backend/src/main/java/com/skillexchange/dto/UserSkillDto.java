package com.skillexchange.dto;

import com.skillexchange.entity.enums.SkillLevel;
import com.skillexchange.entity.enums.SkillType;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserSkillDto {
    private Long id;
    private Long skillId;
    private String skillName;
    private String categoryName;
    private SkillType skillType;
    private SkillLevel level;
    private String experienceDescription;
}
