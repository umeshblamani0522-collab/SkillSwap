
package com.skillexchange.repository;

import com.skillexchange.entity.ChatConnection;
import com.skillexchange.entity.enums.ConnectionStatus;

import jakarta.persistence.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatConnectionRepository
        extends JpaRepository<ChatConnection, Long> {

    @Query("""
                SELECT c FROM ChatConnection c
                WHERE (c.user1.id = :idA AND c.user2.id = :idB)
                   OR (c.user1.id = :idB AND c.user2.id = :idA)
            """)
    Optional<ChatConnection> findConnectionBetween(
            @Param("idA") Long idA,
            @Param("idB") Long idB);

    @Query("""
                SELECT c FROM ChatConnection c
                WHERE (c.user1.id = :userId OR c.user2.id = :userId)
                  AND c.status = :status
                ORDER BY c.updatedAt DESC, c.createdAt DESC
            """)
    List<ChatConnection> findConnectionsForUser(
            @Param("userId") Long userId,
            @Param("status") ConnectionStatus status);

    // Delete chat connections that reference the selected user.
    @Modifying
    @Query("""
                DELETE FROM ChatConnection c
                WHERE c.user1.id = :userId
                   OR c.user2.id = :userId
                   OR c.actionUser.id = :userId
            """)
    int deleteReferencingUser(
            @Param("userId") Long userId);
}
