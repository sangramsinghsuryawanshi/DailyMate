CREATE TABLE assistant_conversations (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    title VARCHAR(120) NOT NULL,
    prompt VARCHAR(5000) NOT NULL,
    response VARCHAR(5000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_assistant_conversations_user_id ON assistant_conversations(user_id);
