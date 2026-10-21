package com.skillexchange.dto.report;

import com.skillexchange.entity.enums.ReportTargetType;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateReportRequest {
    @NotNull
    private ReportTargetType targetType;
    @NotNull
    private Long targetId;
    @NotBlank @Size(max = 1000)
    private String reason;
}
