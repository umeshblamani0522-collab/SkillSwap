package com.skillexchange.dto.exchange;

import com.skillexchange.entity.enums.ExchangeStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExchangeRequestDto {
    private Long id;
    private Long senderId;
    private String senderUsername;
    private Long receiverId;
    private String receiverUsername;
    private Long offeredSkillId;
    private String offeredSkillName;
    private Long requestedSkillId;
    private String requestedSkillName;
    private Double durationHours;
    private String message;
    private ExchangeStatus status;
    private Double matchPercentage;
    private LocalDateTime createdAt;
}
