package com.skillexchange.dto.rating;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RatingDto {
    private Long id;
    private Long sessionId;
    private Long raterId;
    private String raterUsername;
    private Long ratedUserId;
    private Integer stars;
    private String review;
    private LocalDateTime createdAt;
}
