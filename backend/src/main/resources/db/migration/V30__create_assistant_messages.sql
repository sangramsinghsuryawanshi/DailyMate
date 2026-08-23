CREATE TABLE assistant_messages (
    id VARCHAR(64) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content LONGTEXT NOT NULL,
    proposed_action_json LONGTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_assistant_messages_convo (conversation_id),
    INDEX idx_assistant_messages_user (user_id),
    CONSTRAINT fk_assistant_messages_convo FOREIGN KEY (conversation_id) REFERENCES assistant_conversations(id) ON DELETE CASCADE
);

-- Backfill initial turns from existing assistant_conversations
INSERT INTO assistant_messages (id, conversation_id, user_id, role, content, created_at)
SELECT CONCAT(id, '-u'), id, user_id, 'USER', prompt, created_at
FROM assistant_conversations
WHERE prompt IS NOT NULL AND prompt != '';

INSERT INTO assistant_messages (id, conversation_id, user_id, role, content, created_at)
SELECT CONCAT(id, '-a'), id, user_id, 'ASSISTANT', response, created_at
FROM assistant_conversations
WHERE response IS NOT NULL AND response != '';
