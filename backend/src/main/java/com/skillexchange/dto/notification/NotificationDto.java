package com.skillexchange.dto.notification;

import com.skillexchange.entity.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationDto {
    private Long id;
    private NotificationType type;
    private String content;
    private String linkUrl;
    private boolean read;
    private LocalDateTime createdAt;
}
