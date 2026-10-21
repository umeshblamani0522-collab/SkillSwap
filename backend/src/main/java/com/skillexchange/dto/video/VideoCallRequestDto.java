package com.skillexchange.dto.video;

import com.skillexchange.entity.enums.VideoCallRequestStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VideoCallRequestDto {
    private Long id;
    private Long senderId;
    private String senderUsername;
    private String senderFullName;
    private Long receiverId;
    private Long sessionId;
    private String receiverUsername;
    private VideoCallRequestStatus status;
    private String roomId;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
