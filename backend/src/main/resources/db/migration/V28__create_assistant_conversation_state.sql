CREATE TABLE assistant_conversation_state (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL UNIQUE,
    user_id VARCHAR(36) NOT NULL,
    active_proposal_id VARCHAR(36),
    last_action_id VARCHAR(36),
    last_tool_name VARCHAR(120),
    last_entity_type VARCHAR(64),
    last_entity_id VARCHAR(64),
    last_entity_summary VARCHAR(255),
    unresolved_params_json LONGTEXT,
    turn_count INT NOT NULL DEFAULT 1,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    INDEX idx_conv_state_user (user_id),
    INDEX idx_conv_state_conv (conversation_id),
    INDEX idx_conv_state_expires (expires_at)
);
