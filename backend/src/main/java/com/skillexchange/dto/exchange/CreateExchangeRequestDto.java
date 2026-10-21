package com.skillexchange.dto.exchange;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateExchangeRequestDto {
    @NotNull
    private Long receiverId;
    @NotNull
    private Long offeredSkillId;
    @NotNull
    private Long requestedSkillId;
    @NotNull @DecimalMin(value = "0.5")
    private Double durationHours;
    @Size(max = 1000)
    private String message;
}
