CREATE TABLE blocked_emails (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(120) NOT NULL,
    reason VARCHAR(500),
    blocked_by_admin_id BIGINT,
    blocked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_blocked_emails_email UNIQUE (email)
);