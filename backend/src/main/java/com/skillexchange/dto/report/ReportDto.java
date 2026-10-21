package com.skillexchange.dto.report;

import com.skillexchange.entity.enums.ReportStatus;
import com.skillexchange.entity.enums.ReportTargetType;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReportDto {
    private Long id;
    private Long reporterId;
    private String reporterUsername;
    private ReportTargetType targetType;
    private Long targetId;
    private String reason;
    private ReportStatus status;
    private String resolutionNotes;
    private LocalDateTime createdAt;
}
