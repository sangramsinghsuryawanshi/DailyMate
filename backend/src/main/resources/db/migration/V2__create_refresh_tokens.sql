CREATE TABLE refresh_tokens (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
