
package com.skillexchange.repository;

import com.skillexchange.entity.PartnerVideoRoom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PartnerVideoRoomRepository
        extends JpaRepository<PartnerVideoRoom, Long> {

    Optional<PartnerVideoRoom> findByRoomId(String roomId);

    @Query("""
                SELECT r FROM PartnerVideoRoom r
                WHERE (
                    (r.user1.id = :idA AND r.user2.id = :idB)
                    OR
                    (r.user1.id = :idB AND r.user2.id = :idA)
                )
                AND r.expiresAt > :now
                ORDER BY r.createdAt DESC
            """)
    Optional<PartnerVideoRoom> findActiveBetween(
            @Param("idA") Long idA,
            @Param("idB") Long idB,
            @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
                DELETE FROM PartnerVideoRoom r
                WHERE r.expiresAt <= :now
            """)
    int deleteExpired(
            @Param("now") LocalDateTime now);

    @Query("""
                SELECT r FROM PartnerVideoRoom r
                WHERE (r.user1.id = :userId OR r.user2.id = :userId)
                  AND r.expiresAt > :now
                  AND r.endedAt IS NULL
                  AND r.lastJoinedUserId IS NOT NULL
                  AND r.lastJoinedUserId <> :userId
                ORDER BY r.lastJoinedAt DESC
            """)
    List<PartnerVideoRoom> findReadyRoomsForParticipant(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now);

    // Clear references stored as plain user IDs in rooms that remain.
    @Modifying
    @Query("""
                UPDATE PartnerVideoRoom r
                SET r.lastJoinedUserId = NULL
                WHERE r.lastJoinedUserId = :userId
            """)
    int clearLastJoinedUser(
            @Param("userId") Long userId);

    // Delete rooms where the user is a participant or moderator.
    @Modifying
    @Query("""
                DELETE FROM PartnerVideoRoom r
                WHERE r.user1.id = :userId
                   OR r.user2.id = :userId
                   OR r.moderatorUser.id = :userId
            """)
    int deleteReferencingUser(
            @Param("userId") Long userId);
}
