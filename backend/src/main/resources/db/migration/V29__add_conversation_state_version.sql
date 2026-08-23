ALTER TABLE assistant_conversation_state
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
