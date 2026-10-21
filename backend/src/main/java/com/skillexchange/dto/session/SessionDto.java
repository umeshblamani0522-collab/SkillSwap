package com.skillexchange.dto.session;

import com.skillexchange.entity.enums.SessionStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SessionDto {
    private Long id;
    private Long exchangeRequestId;
    private Long teacherId;
    private String teacherUsername;
    private Long learnerId;
    private String learnerUsername;
    private Long skillId;
    private String skillName;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private SessionStatus status;
    private String meetingInfo;
    private String notes;
    private boolean creditsTransferred;
    private LocalDateTime createdAt;
}
