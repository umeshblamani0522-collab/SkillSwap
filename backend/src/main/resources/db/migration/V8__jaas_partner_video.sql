ALTER TABLE partner_video_rooms
    ADD COLUMN IF NOT EXISTS moderator_user_id BIGINT;

UPDATE partner_video_rooms
SET moderator_user_id = user1_id
WHERE moderator_user_id IS NULL;

ALTER TABLE partner_video_rooms
    ALTER COLUMN moderator_user_id SET NOT NULL;

ALTER TABLE partner_video_rooms
    ADD CONSTRAINT fk_partner_video_room_moderator
    FOREIGN KEY (moderator_user_id) REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_partner_video_room_moderator
    ON partner_video_rooms(moderator_user_id);
