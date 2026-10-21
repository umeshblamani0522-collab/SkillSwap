-- Short-lived, participant-scoped video rooms for strong reciprocal matches.
CREATE TABLE IF NOT EXISTS partner_video_rooms (
    id BIGSERIAL PRIMARY KEY,
    room_id VARCHAR(80) NOT NULL UNIQUE,
    jitsi_room_name VARCHAR(140) NOT NULL UNIQUE,
    user1_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user2_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_partner_video_room_users CHECK (user1_id < user2_id)
);

CREATE INDEX IF NOT EXISTS idx_partner_video_room_user1 ON partner_video_rooms(user1_id);
CREATE INDEX IF NOT EXISTS idx_partner_video_room_user2 ON partner_video_rooms(user2_id);
CREATE INDEX IF NOT EXISTS idx_partner_video_room_expires ON partner_video_rooms(expires_at);
