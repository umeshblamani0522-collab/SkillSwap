package com.skillexchange.service;

import com.skillexchange.dto.notification.NotificationDto;
import com.skillexchange.entity.Notification;
import com.skillexchange.entity.User;
import com.skillexchange.entity.enums.NotificationType;
import com.skillexchange.exception.ResourceNotFoundException;
import com.skillexchange.mapper.EntityMapper;
import com.skillexchange.repository.NotificationRepository;
import com.skillexchange.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EntityMapper mapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public NotificationDto notify(Long userId, NotificationType type, String content, String linkUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Notification notification = Notification.builder()
                .user(user).type(type).content(content).linkUrl(linkUrl).read(false)
                .build();
        notification = notificationRepository.save(notification);
        NotificationDto dto = mapper.toDto(notification);

        // Push in real time to a per-user STOMP queue: /user/{username}/queue/notifications
        messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/notifications", dto);
        return dto;
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(mapper::toDto);
    }

    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void delete(Long userId, Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!n.getUser().getId().equals(userId)) {
            throw new com.skillexchange.exception.AccessDeniedCustomException("Not your notification");
        }
        notificationRepository.delete(n);
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!n.getUser().getId().equals(userId)) {
            throw new com.skillexchange.exception.AccessDeniedCustomException("Not your notification");
        }
        n.setRead(true);
        notificationRepository.save(n);
    }
}
