package com.skillexchange.dto.rating;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateRatingRequest {
    @NotNull
    private Long sessionId;
    @NotNull @Min(1) @Max(5)
    private Integer stars;
    @Size(max = 1000)
    private String review;
}
