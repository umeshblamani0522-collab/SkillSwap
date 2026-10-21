package com.skillexchange.dto.session;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateSessionRequest {
    @NotNull
    private Long exchangeRequestId;

    @NotNull
    @FutureOrPresent(message = "Session date cannot be in the past")
    private LocalDate sessionDate;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    @Size(max = 500)
    private String meetingInfo;

    @Size(max = 1000)
    private String notes;
}
