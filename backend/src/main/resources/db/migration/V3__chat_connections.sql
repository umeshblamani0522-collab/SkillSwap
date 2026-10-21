CREATE TABLE chat_connections (
    id BIGSERIAL PRIMARY KEY,
    user1_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user2_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    action_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT unique_connection UNIQUE (user1_id, user2_id),
    CONSTRAINT check_user_order CHECK (user1_id < user2_id)
);

CREATE INDEX idx_chat_conn_user1 ON chat_connections(user1_id);
CREATE INDEX idx_chat_conn_user2 ON chat_connections(user2_id);
