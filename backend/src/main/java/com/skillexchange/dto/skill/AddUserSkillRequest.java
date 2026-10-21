package com.skillexchange.dto.skill;

import com.skillexchange.entity.enums.SkillLevel;
import com.skillexchange.entity.enums.SkillType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AddUserSkillRequest {
    @NotNull
    private Long skillId;
    @NotNull
    private SkillType skillType;
    private SkillLevel level;
    @Size(max = 1000)
    private String experienceDescription;
}
