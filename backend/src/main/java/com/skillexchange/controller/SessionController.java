package com.skillexchange.controller;

import com.skillexchange.dto.session.CreateSessionRequest;
import com.skillexchange.dto.session.SessionDto;
import com.skillexchange.security.CurrentUser;
import com.skillexchange.service.SessionService;
import com.skillexchange.service.JaasTokenService;
import com.skillexchange.service.PartnerVideoRoomService;
import com.skillexchange.dto.video.VideoMeetingTokenDto;
import com.skillexchange.repository.VideoCallRequestRepository;
import com.skillexchange.entity.enums.VideoCallRequestStatus;
import com.skillexchange.repository.SessionRepository;
import com.skillexchange.exception.AccessDeniedCustomException;
import com.skillexchange.exception.BusinessRuleException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final SessionRepository sessionRepository;
    private final JaasTokenService jaasTokenService;
    private final VideoCallRequestRepository videoCallRequestRepository;
    private final PartnerVideoRoomService partnerVideoRoomService;

    @PostMapping
    public ResponseEntity<SessionDto> schedule(@Valid @RequestBody CreateSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.schedule(CurrentUser.id(), request));
    }

    @GetMapping
    public ResponseEntity<List<SessionDto>> mySessions() {
        return ResponseEntity.ok(sessionService.getMySessions(CurrentUser.id()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.getById(CurrentUser.id(), id));
    }

    @GetMapping("/{id}/video-token")
    public ResponseEntity<VideoMeetingTokenDto> videoToken(@PathVariable Long id) {
        Long userId = CurrentUser.id();
        var access = sessionRepository.findVideoSessionAccess(id, userId)
                .orElseThrow(() -> new AccessDeniedCustomException("You are not a participant in this session"));
        if (access.status() == com.skillexchange.entity.enums.SessionStatus.CANCELLED
                || access.status() == com.skillexchange.entity.enums.SessionStatus.NO_SHOW
                || access.status() == com.skillexchange.entity.enums.SessionStatus.COMPLETED) {
            throw new BusinessRuleException("This session is no longer available for video access");
        }
        if (access.meetingInfo() != null && !access.meetingInfo().isBlank()) {
            throw new BusinessRuleException("This session uses an external meeting platform");
        }

        var accepted = videoCallRequestRepository
                .findBySessionIdAndSenderIdAndStatus(id, userId, VideoCallRequestStatus.ACCEPTED)
                .or(() -> videoCallRequestRepository.findBySessionIdAndReceiverIdAndStatus(id, userId, VideoCallRequestStatus.ACCEPTED))
                .orElseThrow(() -> new BusinessRuleException("Accept a video request from your exchange partner before joining"));
        if (accepted.getRoomId() == null || accepted.getRoomId().isBlank()) {
            throw new BusinessRuleException("The accepted video request has no active room");
        }

        var room = partnerVideoRoomService.authorizeJoin(userId, accepted.getRoomId());
        return ResponseEntity.ok(VideoMeetingTokenDto.builder()
                .appId(room.getAppId())
                .roomName(room.getJitsiRoomName())
                .jwt(room.getJwt())
                .useStaging(room.isUseStaging())
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sessionService.delete(CurrentUser.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<SessionDto> complete(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.complete(CurrentUser.id(), id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<SessionDto> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.cancel(CurrentUser.id(), id));
    }

    @PostMapping("/{id}/no-show")
    public ResponseEntity<SessionDto> noShow(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.markNoShow(CurrentUser.id(), id));
    }
}
