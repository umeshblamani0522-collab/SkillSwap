package com.skillexchange.service;

import com.skillexchange.dto.PageResponse;
import com.skillexchange.dto.video.VideoCallRequestDto;
import com.skillexchange.entity.PartnerVideoRoom;
import com.skillexchange.entity.User;
import com.skillexchange.entity.VideoCallRequest;
import com.skillexchange.entity.enums.NotificationType;
import com.skillexchange.entity.enums.VideoCallRequestStatus;
import com.skillexchange.exception.AccessDeniedCustomException;
import com.skillexchange.exception.BusinessRuleException;
import com.skillexchange.exception.ResourceNotFoundException;
import com.skillexchange.repository.UserRepository;
import com.skillexchange.repository.VideoCallRequestRepository;
import com.skillexchange.repository.SessionRepository;
import com.skillexchange.entity.Session;
import com.skillexchange.entity.enums.SessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VideoCallRequestService {
    private static final int REQUEST_TTL_MINUTES = 5;

    private final VideoCallRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final MatchingService matchingService;
    private final NotificationService notificationService;
    private final PartnerVideoRoomService partnerVideoRoomService;
    private final SessionRepository sessionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public VideoCallRequestDto create(Long senderId, Long receiverId) {
        if (senderId.equals(receiverId)) {
            throw new BusinessRuleException("A video introduction requires another participant");
        }
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found"));

        if (!sender.isEnabled() || sender.isSuspended() || !receiver.isEnabled() || receiver.isSuspended()) {
            throw new BusinessRuleException("Video introductions are unavailable for this account");
        }

        var match = matchingService.calculateMatchBetween(senderId, receiverId);
        boolean reciprocal = !match.getTheyCanTeachYouWant().isEmpty()
                && !match.getYouCanTeachTheyWant().isEmpty();
        if (!reciprocal || match.getOverallMatchPercentage() < 60) {
            throw new BusinessRuleException("Video introductions are available only for strong reciprocal matches");
        }
        if (requestRepository.existsBySenderIdAndReceiverIdAndStatus(senderId, receiverId, VideoCallRequestStatus.PENDING)
                || requestRepository.existsBySenderIdAndReceiverIdAndStatus(receiverId, senderId, VideoCallRequestStatus.PENDING)) {
            throw new BusinessRuleException("There is already a pending video introduction request with this partner");
        }

        VideoCallRequest request = VideoCallRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .status(VideoCallRequestStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(REQUEST_TTL_MINUTES))
                .build();
        request = requestRepository.save(request);

        VideoCallRequestDto pendingDto = toDto(request);
        messagingTemplate.convertAndSendToUser(
                receiver.getUsername(),
                "/queue/video-request",
                pendingDto
        );

        notificationService.notify(receiver.getId(), NotificationType.VIDEO_CALL_REQUEST,
                sender.getUsername() + " wants to start a video introduction with you",
                "/video-requests/" + request.getId());

        return toDto(request);
    }

    @Transactional
    public VideoCallRequestDto createForSession(Long senderId, Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (!session.getTeacher().getId().equals(senderId) && !session.getLearner().getId().equals(senderId)) {
            throw new AccessDeniedCustomException("You are not a participant in this session");
        }
        if (session.getStatus() == SessionStatus.CANCELLED || session.getStatus() == SessionStatus.COMPLETED || session.getStatus() == SessionStatus.NO_SHOW) {
            throw new BusinessRuleException("Video access is unavailable for this session");
        }
        if (session.getMeetingInfo() != null && !session.getMeetingInfo().isBlank()) {
            throw new BusinessRuleException("This session uses an external meeting platform");
        }

        Long receiverId = session.getTeacher().getId().equals(senderId)
                ? session.getLearner().getId() : session.getTeacher().getId();

        if (requestRepository.findBySessionIdAndSenderIdAndStatus(sessionId, senderId, VideoCallRequestStatus.PENDING).isPresent()
                || requestRepository.findBySessionIdAndReceiverIdAndStatus(sessionId, senderId, VideoCallRequestStatus.PENDING).isPresent()) {
            throw new BusinessRuleException("There is already a pending video request for this session");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found"));

        if (!sender.isEnabled() || sender.isSuspended() || !receiver.isEnabled() || receiver.isSuspended()) {
            throw new BusinessRuleException("Video introductions are unavailable for this account");
        }

        VideoCallRequest request = VideoCallRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .session(session)
                .status(VideoCallRequestStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(REQUEST_TTL_MINUTES))
                .build();
        request = requestRepository.save(request);

        VideoCallRequestDto pendingDto = toDto(request);
        messagingTemplate.convertAndSendToUser(
                receiver.getUsername(),
                "/queue/video-request",
                pendingDto
        );

        notificationService.notify(receiver.getId(), NotificationType.VIDEO_CALL_REQUEST,
                sender.getUsername() + " wants to join your " + session.getSkill().getName() + " SkillSwap video session",
                "/video-requests/" + request.getId());
        return toDto(request);
    }

    @Transactional
    public VideoCallRequestDto accept(Long receiverId, Long requestId) {
        VideoCallRequest request = requestRepository.findByIdAndReceiverId(requestId, receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Video request not found"));
        if (request.getStatus() != VideoCallRequestStatus.PENDING) {
            throw new BusinessRuleException("This video request is already " + request.getStatus().name().toLowerCase());
        }
        if (request.getExpiresAt().isBefore(LocalDateTime.now())) {
            request.setStatus(VideoCallRequestStatus.EXPIRED);
            requestRepository.save(request);
            throw new BusinessRuleException("This video request has expired");
        }

        PartnerVideoRoom room = partnerVideoRoomService.createRoomForAcceptedRequest(request.getSender().getId(), request.getReceiver().getId());
        request.setStatus(VideoCallRequestStatus.ACCEPTED);
        request.setRoomId(room.getRoomId());
        request.setRespondedAt(LocalDateTime.now());
        requestRepository.save(request);

        // The requester receives the acceptance notification and automatically joins
        // the same private room. The receiver is already redirected by the accept
        // action itself, so no second navigation-triggering notification is sent.
        notificationService.notify(request.getSender().getId(), NotificationType.VIDEO_CALL_REQUEST_ACCEPTED,
                request.getReceiver().getUsername() + " accepted your video introduction request",
                "/partner-video/" + room.getRoomId());

        return toDto(request);
    }

    @Transactional
    public VideoCallRequestDto reject(Long receiverId, Long requestId) {
        VideoCallRequest request = requestRepository.findByIdAndReceiverId(requestId, receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Video request not found"));
        if (request.getStatus() != VideoCallRequestStatus.PENDING) {
            throw new BusinessRuleException("This video request is already " + request.getStatus().name().toLowerCase());
        }
        request.setStatus(VideoCallRequestStatus.REJECTED);
        request.setRespondedAt(LocalDateTime.now());
        requestRepository.save(request);
        notificationService.notify(request.getSender().getId(), NotificationType.VIDEO_CALL_REQUEST_REJECTED,
                request.getReceiver().getUsername() + " declined your video introduction request", "/matches");
        return toDto(request);
    }

    @Transactional
    public VideoCallRequestDto cancel(Long senderId, Long requestId) {
        VideoCallRequest request = requestRepository.findByIdAndSenderId(requestId, senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Video request not found"));
        if (request.getStatus() != VideoCallRequestStatus.PENDING) {
            throw new BusinessRuleException("Only pending video requests can be cancelled");
        }
        request.setStatus(VideoCallRequestStatus.CANCELLED);
        request.setRespondedAt(LocalDateTime.now());
        return toDto(requestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public PageResponse<VideoCallRequestDto> incoming(Long receiverId, int page, int size) {
        return PageResponse.of(requestRepository
                .findByReceiverIdAndStatusOrderByCreatedAtDesc(receiverId, VideoCallRequestStatus.PENDING, PageRequest.of(page, size))
                .map(this::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<VideoCallRequestDto> mine(Long userId, int page, int size) {
        return PageResponse.of(requestRepository
                .findBySenderIdOrReceiverIdOrderByCreatedAtDesc(userId, userId, PageRequest.of(page, size))
                .map(this::toDto));
    }

    @Scheduled(fixedDelayString = "${app.video-request.cleanup-ms:60000}")
    @Transactional
    public void expireRequests() {
        requestRepository.expirePending(LocalDateTime.now());
    }

    private VideoCallRequestDto toDto(VideoCallRequest r) {
        return VideoCallRequestDto.builder()
                .id(r.getId())
                .senderId(r.getSender().getId())
                .senderUsername(r.getSender().getUsername())
                .senderFullName(r.getSender().getFullName())
                .receiverId(r.getReceiver().getId())
                .sessionId(r.getSession() == null ? null : r.getSession().getId())
                .receiverUsername(r.getReceiver().getUsername())
                .status(r.getStatus())
                .roomId(r.getRoomId())
                .expiresAt(r.getExpiresAt())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
