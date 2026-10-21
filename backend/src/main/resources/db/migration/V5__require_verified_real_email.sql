-- New accounts must prove ownership of their email before sign-in.
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Remove only legacy sample accounts from older builds. Do not delete real user data.
CREATE TEMP TABLE legacy_sample_users ON COMMIT DROP AS
SELECT id FROM users WHERE email LIKE '%@skillexchange.dev';

DELETE FROM ratings
WHERE rater_id IN (SELECT id FROM legacy_sample_users)
   OR rated_user_id IN (SELECT id FROM legacy_sample_users)
   OR session_id IN (
       SELECT s.id FROM sessions s
       WHERE s.teacher_id IN (SELECT id FROM legacy_sample_users)
          OR s.learner_id IN (SELECT id FROM legacy_sample_users)
          OR s.exchange_request_id IN (SELECT er.id FROM exchange_requests er
              WHERE er.sender_id IN (SELECT id FROM legacy_sample_users)
                 OR er.receiver_id IN (SELECT id FROM legacy_sample_users))
   );
DELETE FROM sessions
WHERE teacher_id IN (SELECT id FROM legacy_sample_users)
   OR learner_id IN (SELECT id FROM legacy_sample_users)
   OR exchange_request_id IN (SELECT er.id FROM exchange_requests er
       WHERE er.sender_id IN (SELECT id FROM legacy_sample_users)
          OR er.receiver_id IN (SELECT id FROM legacy_sample_users));
DELETE FROM exchange_requests
WHERE sender_id IN (SELECT id FROM legacy_sample_users)
   OR receiver_id IN (SELECT id FROM legacy_sample_users);
DELETE FROM reports WHERE reporter_id IN (SELECT id FROM legacy_sample_users);
DELETE FROM notifications WHERE user_id IN (SELECT id FROM legacy_sample_users);
DELETE FROM messages
WHERE sender_id IN (SELECT id FROM legacy_sample_users)
   OR receiver_id IN (SELECT id FROM legacy_sample_users);
DELETE FROM chat_connections
WHERE user1_id IN (SELECT id FROM legacy_sample_users)
   OR user2_id IN (SELECT id FROM legacy_sample_users)
   OR action_user_id IN (SELECT id FROM legacy_sample_users);
DELETE FROM availability WHERE user_id IN (SELECT id FROM legacy_sample_users);
DELETE FROM user_skills WHERE user_id IN (SELECT id FROM legacy_sample_users);
DELETE FROM credit_transactions
WHERE account_id IN (
    SELECT ca.id FROM credit_accounts ca
    WHERE ca.user_id IN (SELECT id FROM legacy_sample_users)
);
DELETE FROM credit_accounts WHERE user_id IN (SELECT id FROM legacy_sample_users);
DELETE FROM user_roles WHERE user_id IN (SELECT id FROM legacy_sample_users);
DELETE FROM password_reset_tokens WHERE user_id IN (SELECT id FROM legacy_sample_users);
DELETE FROM users WHERE id IN (SELECT id FROM legacy_sample_users);

CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_email_verification_token_hash ON email_verification_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_email_verification_user ON email_verification_tokens(user_id);
