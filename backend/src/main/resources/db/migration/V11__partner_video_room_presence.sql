ALTER TABLE partner_video_rooms
    ADD COLUMN IF NOT EXISTS last_joined_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS last_joined_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS ended_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_partner_video_room_last_joined
    ON partner_video_rooms(last_joined_user_id, last_joined_at);

CREATE INDEX IF NOT EXISTS idx_partner_video_room_ended
    ON partner_video_rooms(ended_at);
