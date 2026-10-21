package com.skillexchange.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "partner_video_rooms", indexes = {
        @Index(name = "idx_partner_video_room_user1", columnList = "user1_id"),
        @Index(name = "idx_partner_video_room_user2", columnList = "user2_id"),
        @Index(name = "idx_partner_video_room_expires", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerVideoRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false, unique = true, length = 80)
    private String roomId;

    @Column(name = "jitsi_room_name", nullable = false, unique = true, length = 140)
    private String jitsiRoomName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user1_id")
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user2_id")
    private User user2;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "moderator_user_id", nullable = false)
    private User moderatorUser;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "last_joined_user_id")
    private Long lastJoinedUserId;

    @Column(name = "last_joined_at")
    private LocalDateTime lastJoinedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
