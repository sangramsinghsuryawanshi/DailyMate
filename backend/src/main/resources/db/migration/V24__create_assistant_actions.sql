CREATE TABLE assistant_actions (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    parameters_json TEXT NOT NULL,
    summary VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    idempotency_key VARCHAR(100) NULL,
    result_message TEXT NULL,
    executed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_assistant_actions_user_id ON assistant_actions(user_id);
CREATE INDEX idx_assistant_actions_status ON assistant_actions(status);
