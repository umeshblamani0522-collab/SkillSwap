package com.skillexchange.dto.skill;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SkillCategoryDto {
    private Long id;
    private String name;
    private String description;
}
