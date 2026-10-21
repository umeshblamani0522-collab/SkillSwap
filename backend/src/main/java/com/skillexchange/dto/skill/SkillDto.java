package com.skillexchange.dto.skill;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SkillDto {
    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private boolean active;
}
