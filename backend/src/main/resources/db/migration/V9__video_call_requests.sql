CREATE TABLE IF NOT EXISTS video_call_requests (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    room_id VARCHAR(80),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    responded_at TIMESTAMP,
    CONSTRAINT chk_video_request_users CHECK (sender_id <> receiver_id)
);

CREATE INDEX IF NOT EXISTS idx_video_request_receiver_status ON video_call_requests(receiver_id, status);
CREATE INDEX IF NOT EXISTS idx_video_request_sender_status ON video_call_requests(sender_id, status);
CREATE INDEX IF NOT EXISTS idx_video_request_expires ON video_call_requests(expires_at);
CREATE UNIQUE INDEX IF NOT EXISTS uq_video_request_pending_pair
    ON video_call_requests(sender_id, receiver_id)
    WHERE status = 'PENDING';
