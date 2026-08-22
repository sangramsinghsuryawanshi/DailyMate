CREATE INDEX idx_assistant_actions_status_expires_at ON assistant_actions(status, expires_at);
CREATE INDEX idx_assistant_actions_user_status ON assistant_actions(user_id, status);
