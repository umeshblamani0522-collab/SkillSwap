-- Engineering integrity and query-performance hardening.
-- Safe to apply to existing installations.

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_lower ON users (LOWER(email));
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_username_lower ON users (LOWER(username));

ALTER TABLE exchange_requests
    ADD CONSTRAINT chk_exchange_duration_range
    CHECK (duration_hours > 0 AND duration_hours <= 8);

CREATE INDEX IF NOT EXISTS idx_exchange_requests_status_expiry
    ON exchange_requests(status, expires_at);

CREATE INDEX IF NOT EXISTS idx_sessions_exchange_status
    ON sessions(exchange_request_id, status);

CREATE INDEX IF NOT EXISTS idx_sessions_participant_schedule
    ON sessions(session_date, start_time, end_time, status);

CREATE INDEX IF NOT EXISTS idx_messages_receiver_created
    ON messages(receiver_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ratings_session_rater
    ON ratings(session_id, rater_id);

CREATE INDEX IF NOT EXISTS idx_user_skills_user_type
    ON user_skills(user_id, skill_type);

CREATE INDEX IF NOT EXISTS idx_availability_user_day
    ON availability(user_id, day_of_week, start_time, end_time);
