
package com.skillexchange.service;

import com.skillexchange.dto.match.MatchResultDto;
import com.skillexchange.dto.match.PartnerVideoRoomDto;
import com.skillexchange.entity.PartnerVideoRoom;
import com.skillexchange.entity.User;
import com.skillexchange.entity.VideoCallRequest;
import com.skillexchange.entity.enums.VideoCallRequestStatus;
import com.skillexchange.exception.AccessDeniedCustomException;
import com.skillexchange.exception.BusinessRuleException;
import com.skillexchange.exception.ResourceNotFoundException;
import com.skillexchange.repository.PartnerVideoRoomRepository;
import com.skillexchange.repository.UserRepository;
import com.skillexchange.repository.SessionRepository;
import com.skillexchange.repository.VideoCallRequestRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PartnerVideoRoomService {

        @Value("${app.video-room.min-match-score:60}")
        private double minMatchScoreForVideo;

        @Value("${app.video-room.ttl-minutes:30}")
        private int roomTtlMinutes;

        private final PartnerVideoRoomRepository roomRepository;
        private final UserRepository userRepository;
        private final MatchingService matchingService;
        private final SecureRandom secureRandom = new SecureRandom();
        private final JaasTokenService jaasTokenService;
        private final SessionRepository sessionRepository;
        private final VideoCallRequestRepository videoCallRequestRepository;

        @Transactional
        public PartnerVideoRoom createOrReuseEntity(
                        Long userId,
                        Long partnerId) {

                return createRoom(
                                userId,
                                partnerId,
                                userId,
                                randomToken(),
                                true);
        }

        /**
         * Creates a room for an accepted video request.
         */
        @Transactional
        public PartnerVideoRoom createRoomForAcceptedRequest(
                        Long senderId,
                        Long receiverId) {

                return createRoom(
                                senderId,
                                receiverId,
                                receiverId,
                                randomToken(),
                                false);
        }

        private PartnerVideoRoom createRoom(
                        Long userId,
                        Long partnerId,
                        Long moderatorId,
                        String roomId,
                        boolean enforceStrongMatch) {

                if (userId.equals(partnerId)) {
                        throw new BusinessRuleException(
                                        "A partner video call requires another participant");
                }

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                User partner = userRepository.findById(partnerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Partner not found"));

                if (!user.isEnabled() || user.isSuspended()
                                || !partner.isEnabled() || partner.isSuspended()) {
                        throw new BusinessRuleException(
                                        "Video calls are unavailable for this account");
                }

                if (enforceStrongMatch) {
                        MatchResultDto match = matchingService.calculateMatchBetween(
                                        userId, partnerId);

                        boolean reciprocal = !match.getTheyCanTeachYouWant().isEmpty()
                                        && !match.getYouCanTeachTheyWant().isEmpty();

                        if (!reciprocal
                                        || match.getOverallMatchPercentage() < minMatchScoreForVideo) {
                                throw new BusinessRuleException(
                                                "Partner video calls are available only for strong reciprocal matches");
                        }
                }

                LocalDateTime now = LocalDateTime.now();

                Long first = Math.min(userId, partnerId);

                User user1 = first.equals(userId) ? user : partner;
                User user2 = first.equals(userId) ? partner : user;
                User moderator = moderatorId.equals(userId) ? user : partner;

                String jitsiRoomName = "SkillSwap-Partner-" + roomId;

                PartnerVideoRoom room = PartnerVideoRoom.builder()
                                .roomId(roomId)
                                .jitsiRoomName(jitsiRoomName)
                                .user1(user1)
                                .user2(user2)
                                .moderatorUser(moderator)
                                .expiresAt(now.plusMinutes(roomTtlMinutes))
                                .build();

                return roomRepository.save(room);
        }

        @Transactional(readOnly = true)
        public PartnerVideoRoom getRoomForParticipant(
                        Long userId,
                        String roomId) {

                PartnerVideoRoom room = roomRepository.findByRoomId(roomId)
                                .orElseThrow(() -> new ResourceNotFoundException("Video room not found"));

                if (!room.getUser1().getId().equals(userId)
                                && !room.getUser2().getId().equals(userId)) {
                        throw new AccessDeniedCustomException(
                                        "You are not a participant in this video room");
                }

                return room;
        }

        /**
         * Authorizes access to an accepted video request.
         *
         * Ended rooms are final and cannot be reopened. If an active request
         * points to a missing or expired room, a fresh room ID and Jitsi room
         * name are generated instead of reusing the old room.
         */
        @Transactional
        public PartnerVideoRoomDto authorizeJoin(
                        Long userId,
                        String roomId) {

                VideoCallRequest acceptedRequest = videoCallRequestRepository.findByRoomIdAndStatusForUpdate(
                                roomId,
                                VideoCallRequestStatus.ACCEPTED).orElse(null);

                PartnerVideoRoom room = roomRepository.findByRoomId(roomId).orElse(null);

                if (acceptedRequest != null
                                && !acceptedRequest.getSender().getId().equals(userId)
                                && !acceptedRequest.getReceiver().getId().equals(userId)) {
                        throw new AccessDeniedCustomException(
                                        "You are not a participant in this video room");
                }

                // Never reactivate an ended room.
                if (room != null && room.getEndedAt() != null) {
                        throw new BusinessRuleException(
                                        "This video session has ended. Start a new video request to call again.");
                }

                LocalDateTime now = LocalDateTime.now();

                // A missing or expired room needs a NEW ID and a NEW Jitsi room name.
                if (acceptedRequest != null
                                && (room == null
                                                || !room.getExpiresAt().isAfter(now))) {

                        room = createFreshRoomForAcceptedRequest(acceptedRequest);

                } else if (room == null) {

                        throw new ResourceNotFoundException("Video room not found");
                }

                if (!room.getExpiresAt().isAfter(LocalDateTime.now())) {
                        throw new BusinessRuleException(
                                        "This partner video room has expired");
                }

                if (room.getEndedAt() != null) {
                        throw new BusinessRuleException(
                                        "This video session has ended. Start a new video request to call again.");
                }

                if (!room.getUser1().getId().equals(userId)
                                && !room.getUser2().getId().equals(userId)) {
                        throw new AccessDeniedCustomException(
                                        "You are not a participant in this video room");
                }

                if (!videoCallRequestRepository.existsByRoomIdAndStatus(
                                room.getRoomId(),
                                VideoCallRequestStatus.ACCEPTED)) {
                        throw new BusinessRuleException(
                                        "This video room is not linked to an accepted video introduction request");
                }

                room.setLastJoinedUserId(userId);
                room.setLastJoinedAt(LocalDateTime.now());

                roomRepository.save(room);

                return toDtoFor(room, userId);
        }

        /**
         * Creates a fresh room for an existing accepted request without reusing
         * an old Jitsi room name.
         */
        private PartnerVideoRoom createFreshRoomForAcceptedRequest(
                        VideoCallRequest acceptedRequest) {

                String newRoomId = randomToken();

                PartnerVideoRoom room = createRoom(
                                acceptedRequest.getSender().getId(),
                                acceptedRequest.getReceiver().getId(),
                                acceptedRequest.getReceiver().getId(),
                                newRoomId,
                                false);

                acceptedRequest.setRoomId(newRoomId);
                videoCallRequestRepository.save(acceptedRequest);

                return room;
        }

        /**
         * Finds the active room associated with a scheduled session.
         * Ended sessions are not automatically reopened.
         */
        @Transactional
        public String prepareSessionRoom(
                        Long userId,
                        Long sessionId) {

                sessionRepository.findVideoSessionAccess(sessionId, userId)
                                .orElseThrow(() -> new AccessDeniedCustomException(
                                                "You are not a participant in this session"));

                List<VideoCallRequest> acceptedRequests = videoCallRequestRepository
                                .findAcceptedBySessionAndParticipantForUpdate(
                                                sessionId,
                                                userId,
                                                VideoCallRequestStatus.ACCEPTED);

                if (acceptedRequests.isEmpty()) {
                        throw new BusinessRuleException(
                                        "Accept a video request from your exchange partner before joining");
                }

                VideoCallRequest acceptedRequest = acceptedRequests.get(0);
                String currentRoomId = acceptedRequest.getRoomId();

                PartnerVideoRoom room = currentRoomId == null
                                ? null
                                : roomRepository.findByRoomId(currentRoomId).orElse(null);

                // Do not clear endedAt or reopen an ended meeting.
                if (room != null && room.getEndedAt() != null) {
                        throw new BusinessRuleException(
                                        "This video session has ended. Start a new video request to call again.");
                }

                if (room == null
                                || !room.getExpiresAt().isAfter(LocalDateTime.now())) {

                        room = createFreshRoomForAcceptedRequest(acceptedRequest);
                }

                return room.getRoomId();
        }

        @Transactional
        public PartnerVideoRoom endRoom(
                        Long userId,
                        String roomId) {

                PartnerVideoRoom room = getRoomForParticipant(userId, roomId);

                // Repeated end requests are harmless.
                if (room.getEndedAt() == null) {
                        room.setEndedAt(LocalDateTime.now());
                        room.setLastJoinedUserId(null);
                        room.setLastJoinedAt(null);
                }

                return roomRepository.save(room);
        }

        @Transactional(readOnly = true)
        public List<String> getReadyRoomIds(Long userId) {

                return roomRepository
                                .findReadyRoomsForParticipant(
                                                userId,
                                                LocalDateTime.now())
                                .stream()
                                .map(PartnerVideoRoom::getRoomId)
                                .toList();
        }

        @Scheduled(fixedDelayString = "${app.video-room.cleanup-ms:3600000}", initialDelayString = "${app.video-room.cleanup-initial-delay-ms:60000}")
        @Transactional
        public void removeExpiredRooms() {
                roomRepository.deleteExpired(LocalDateTime.now());
        }

        private PartnerVideoRoomDto toDtoFor(
                        PartnerVideoRoom room,
                        Long viewerId) {

                if (!room.getUser1().getId().equals(viewerId)
                                && !room.getUser2().getId().equals(viewerId)) {
                        throw new AccessDeniedCustomException(
                                        "You are not a participant in this video room");
                }

                User participant = room.getUser1().getId().equals(viewerId)
                                ? room.getUser1()
                                : room.getUser2();

                User partner = room.getUser1().getId().equals(viewerId)
                                ? room.getUser2()
                                : room.getUser1();

                boolean moderator = room.getModeratorUser().getId().equals(viewerId);

                var participantDto = new com.skillexchange.dto.video.JaasParticipantDto(
                                participant.getId(),
                                participant.getUsername(),
                                participant.getFullName(),
                                participant.getEmail(),
                                moderator);

                var token = jaasTokenService.createToken(
                                participantDto,
                                room.getJitsiRoomName(),
                                moderator);

                return PartnerVideoRoomDto.builder()
                                .roomId(room.getRoomId())
                                .jitsiRoomName(token.getRoomName())
                                .partnerId(partner.getId())
                                .partnerUsername(partner.getUsername())
                                .expiresAt(room.getExpiresAt())
                                .appId(token.getAppId())
                                .jwt(token.getJwt())
                                .useStaging(token.isUseStaging())
                                .build();
        }

        private String randomToken() {
                byte[] bytes = new byte[24];
                secureRandom.nextBytes(bytes);
                return HexFormat.of().formatHex(bytes);
        }
}
