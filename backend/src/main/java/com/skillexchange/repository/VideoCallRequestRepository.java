package com.skillexchange.repository;

import com.skillexchange.entity.VideoCallRequest;
import com.skillexchange.entity.enums.VideoCallRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

import java.util.List;

public interface VideoCallRequestRepository extends JpaRepository<VideoCallRequest, Long> {
        Optional<VideoCallRequest> findByIdAndReceiverId(Long id, Long receiverId);

        Optional<VideoCallRequest> findByIdAndSenderId(Long id, Long senderId);

        boolean existsByRoomIdAndStatus(String roomId, VideoCallRequestStatus status);

        Optional<VideoCallRequest> findByRoomIdAndStatus(String roomId, VideoCallRequestStatus status);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT r FROM VideoCallRequest r WHERE r.roomId = :roomId AND r.status = :status")
        Optional<VideoCallRequest> findByRoomIdAndStatusForUpdate(@Param("roomId") String roomId,
                        @Param("status") VideoCallRequestStatus status);

        Optional<VideoCallRequest> findBySessionIdAndSenderIdAndStatus(Long sessionId, Long senderId,
                        VideoCallRequestStatus status);

        Optional<VideoCallRequest> findBySessionIdAndReceiverIdAndStatus(Long sessionId, Long receiverId,
                        VideoCallRequestStatus status);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT r FROM VideoCallRequest r WHERE r.session.id = :sessionId AND r.status = :status " +
                        "AND (r.sender.id = :userId OR r.receiver.id = :userId) ORDER BY r.createdAt DESC")
        java.util.List<VideoCallRequest> findAcceptedBySessionAndParticipantForUpdate(
                        @Param("sessionId") Long sessionId,
                        @Param("userId") Long userId,
                        @Param("status") VideoCallRequestStatus status);

        boolean existsBySenderIdAndReceiverIdAndStatus(Long senderId, Long receiverId, VideoCallRequestStatus status);

        Page<VideoCallRequest> findByReceiverIdAndStatusOrderByCreatedAtDesc(Long receiverId,
                        VideoCallRequestStatus status, Pageable pageable);

        Page<VideoCallRequest> findBySenderIdOrReceiverIdOrderByCreatedAtDesc(Long senderId, Long receiverId,
                        Pageable pageable);

        @Modifying
        @Query("UPDATE VideoCallRequest r SET r.status = com.skillexchange.entity.enums.VideoCallRequestStatus.EXPIRED "
                        + "WHERE r.status = com.skillexchange.entity.enums.VideoCallRequestStatus.PENDING AND r.expiresAt <= :now")
        int expirePending(@Param("now") LocalDateTime now);

        void deleteBySenderIdOrReceiverId(
                        Long senderId, Long receiverId);

        void deleteBySessionIdIn(List<Long> sessionIds);
}
