ALTER TABLE video_call_requests
    ADD COLUMN IF NOT EXISTS session_id BIGINT REFERENCES sessions(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_video_request_session_status
    ON video_call_requests(session_id, status);

CREATE UNIQUE INDEX IF NOT EXISTS uq_video_request_pending_session
    ON video_call_requests(session_id)
    WHERE status = 'PENDING' AND session_id IS NOT NULL;
