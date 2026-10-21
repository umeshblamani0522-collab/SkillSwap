package com.skillexchange.dto.report;

import com.skillexchange.entity.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResolveReportRequest {
    @NotNull
    private ReportStatus status;
    private String resolutionNotes;
}
