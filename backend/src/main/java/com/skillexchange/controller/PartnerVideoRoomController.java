package com.skillexchange.controller;

import com.skillexchange.dto.match.PartnerVideoRoomDto;
import com.skillexchange.security.CurrentUser;
import com.skillexchange.service.PartnerVideoRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/partner-video")
@RequiredArgsConstructor
public class PartnerVideoRoomController {

    private final PartnerVideoRoomService partnerVideoRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<PartnerVideoRoomDto> authorize(@PathVariable String roomId) {
        Long userId = CurrentUser.id();
        PartnerVideoRoomDto dto = partnerVideoRoomService.authorizeJoin(userId, roomId);

        // authorizeJoin builds the DTO inside the service transaction, so all
        // lazy User fields needed for the notification are already resolved.
        // Do not load the PartnerVideoRoom again here: doing so after the
        // transaction closes can trigger Hibernate LazyInitializationException.
        messagingTemplate.convertAndSendToUser(dto.getPartnerUsername(), "/queue/video-room", Map.of(
                "type", "VIDEO_ROOM_PARTICIPANT_READY",
                "roomId", dto.getRoomId(),
                "joinedByUserId", userId,
                "participantUserId", dto.getPartnerId()
        ));

        return ResponseEntity.ok(dto);
    }

    /**
     * Resolve a session's current accepted video room. The browser may hold a
     * stale room id after the old 30-minute room expired, so the server is the
     * source of truth for the room used by the Session page.
     */
    @GetMapping("/sessions/{sessionId}/room")
    public ResponseEntity<Map<String, String>> prepareSessionRoom(@PathVariable Long sessionId) {
        String roomId = partnerVideoRoomService.prepareSessionRoom(CurrentUser.id(), sessionId);
        return ResponseEntity.ok(Map.of("roomId", roomId));
    }

    /**
     * Lightweight fallback for clients that miss the STOMP participant-ready
     * event. It returns rooms where the other participant has joined recently.
     */
    @GetMapping("/ready")
    public ResponseEntity<List<String>> readyRooms() {
        return ResponseEntity.ok(partnerVideoRoomService.getReadyRoomIds(CurrentUser.id()));
    }

    @PostMapping("/rooms/{roomId}/end")
    public ResponseEntity<Void> endRoom(@PathVariable String roomId) {
        Long userId = CurrentUser.id();
        var room = partnerVideoRoomService.endRoom(userId, roomId);
        messagingTemplate.convertAndSend("/topic/video-room/" + roomId, Map.of(
                "type", "VIDEO_ROOM_ENDED",
                "roomId", roomId,
                "endedByUserId", userId
        ));
        return ResponseEntity.noContent().build();
    }
}
