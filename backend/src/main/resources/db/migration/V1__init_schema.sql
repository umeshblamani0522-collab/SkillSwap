-- ============================================================
-- Skill Exchange Platform - Initial schema
-- ============================================================

CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE users (
    id                   BIGSERIAL PRIMARY KEY,
    username             VARCHAR(50)  NOT NULL UNIQUE,
    email                VARCHAR(120) NOT NULL UNIQUE,
    password             VARCHAR(255) NOT NULL,
    full_name            VARCHAR(120),
    phone                VARCHAR(20),
    bio                  VARCHAR(1000),
    location             VARCHAR(120),
    profile_picture_url  VARCHAR(500),
    languages            VARCHAR(200),
    experience_years     INTEGER,
    is_suspended         BOOLEAN NOT NULL DEFAULT FALSE,
    is_enabled           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_location ON users(location);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE skill_categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500)
);

CREATE TABLE skills (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL UNIQUE,
    category_id BIGINT REFERENCES skill_categories(id) ON DELETE SET NULL,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_skills_category ON skills(category_id);
CREATE INDEX idx_skills_active ON skills(is_active);

CREATE TABLE user_skills (
    id                       BIGSERIAL PRIMARY KEY,
    user_id                  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill_id                 BIGINT NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    skill_type               VARCHAR(10) NOT NULL CHECK (skill_type IN ('TEACH','LEARN')),
    skill_level              VARCHAR(20) CHECK (skill_level IN ('BEGINNER','INTERMEDIATE','ADVANCED','EXPERT')),
    experience_description   VARCHAR(1000),
    UNIQUE (user_id, skill_id, skill_type)
);
CREATE INDEX idx_user_skills_user ON user_skills(user_id);
CREATE INDEX idx_user_skills_skill_type ON user_skills(skill_id, skill_type);

CREATE TABLE availability (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    day_of_week  VARCHAR(10) NOT NULL,
    start_time   TIME NOT NULL,
    end_time     TIME NOT NULL,
    timezone     VARCHAR(60)
);
CREATE INDEX idx_availability_user ON availability(user_id);

CREATE TABLE exchange_requests (
    id                  BIGSERIAL PRIMARY KEY,
    sender_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    offered_skill_id    BIGINT NOT NULL REFERENCES skills(id),
    requested_skill_id  BIGINT NOT NULL REFERENCES skills(id),
    duration_hours      DOUBLE PRECISION NOT NULL,
    message             VARCHAR(1000),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING','ACCEPTED','REJECTED','CANCELLED','EXPIRED','COMPLETED')),
    match_percentage    DOUBLE PRECISION,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now(),
    expires_at          TIMESTAMP,
    CHECK (sender_id <> receiver_id)
);
CREATE INDEX idx_exchange_requests_receiver ON exchange_requests(receiver_id, status);
CREATE INDEX idx_exchange_requests_sender ON exchange_requests(sender_id);

CREATE TABLE sessions (
    id                    BIGSERIAL PRIMARY KEY,
    exchange_request_id   BIGINT NOT NULL REFERENCES exchange_requests(id) ON DELETE CASCADE,
    teacher_id            BIGINT NOT NULL REFERENCES users(id),
    learner_id            BIGINT NOT NULL REFERENCES users(id),
    skill_id              BIGINT NOT NULL REFERENCES skills(id),
    session_date          DATE NOT NULL,
    start_time            TIME NOT NULL,
    end_time              TIME NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
                           CHECK (status IN ('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED','NO_SHOW')),
    meeting_info          VARCHAR(500),
    notes                 VARCHAR(1000),
    credits_transferred   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP NOT NULL DEFAULT now(),
    CHECK (start_time < end_time)
);
CREATE INDEX idx_sessions_teacher ON sessions(teacher_id, session_date);
CREATE INDEX idx_sessions_learner ON sessions(learner_id, session_date);

CREATE TABLE messages (
    id           BIGSERIAL PRIMARY KEY,
    sender_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content      VARCHAR(2000) NOT NULL,
    is_read      BOOLEAN NOT NULL DEFAULT FALSE,
    read_at      TIMESTAMP,
    created_at   TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_messages_conversation ON messages(sender_id, receiver_id, created_at);
CREATE INDEX idx_messages_receiver_unread ON messages(receiver_id, is_read);

CREATE TABLE notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        VARCHAR(40) NOT NULL,
    content     VARCHAR(500) NOT NULL,
    link_url    VARCHAR(300),
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_unread ON notifications(user_id, is_read);

CREATE TABLE credit_accounts (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    balance       NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    total_earned  NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_spent   NUMERIC(12,2) NOT NULL DEFAULT 0,
    version       BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE credit_transactions (
    id               BIGSERIAL PRIMARY KEY,
    account_id       BIGINT NOT NULL REFERENCES credit_accounts(id) ON DELETE CASCADE,
    type             VARCHAR(30) NOT NULL
                     CHECK (type IN ('EARNED_TEACHING','SPENT_LEARNING','ADJUSTMENT_ADMIN','REFUND')),
    amount           NUMERIC(12,2) NOT NULL,
    balance_after    NUMERIC(12,2) NOT NULL,
    related_session_id BIGINT REFERENCES sessions(id) ON DELETE SET NULL,
    description      VARCHAR(300),
    idempotency_key  VARCHAR(100) NOT NULL UNIQUE,
    created_at       TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_credit_transactions_account ON credit_transactions(account_id, created_at DESC);

CREATE TABLE ratings (
    id           BIGSERIAL PRIMARY KEY,
    session_id   BIGINT NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    rater_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rated_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    stars        INTEGER NOT NULL CHECK (stars BETWEEN 1 AND 5),
    review       VARCHAR(1000),
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (session_id, rater_id)
);
CREATE INDEX idx_ratings_rated_user ON ratings(rated_user_id, created_at DESC);

CREATE TABLE reports (
    id                 BIGSERIAL PRIMARY KEY,
    reporter_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_type        VARCHAR(20) NOT NULL CHECK (target_type IN ('USER','MESSAGE','SESSION','CONTENT')),
    target_id          BIGINT NOT NULL,
    reason             VARCHAR(1000) NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                       CHECK (status IN ('OPEN','UNDER_REVIEW','RESOLVED','REJECTED')),
    resolution_notes   VARCHAR(1000),
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_reports_status ON reports(status, created_at DESC);

CREATE TABLE audit_logs (
    id                  BIGSERIAL PRIMARY KEY,
    admin_id            BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action              VARCHAR(100) NOT NULL,
    target_description  VARCHAR(300),
    details             VARCHAR(2000),
    created_at          TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at DESC);
