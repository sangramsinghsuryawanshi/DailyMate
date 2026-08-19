CREATE TABLE notifications (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    title VARCHAR(120) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    type VARCHAR(40) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_notifications_user_created (user_id, created_at DESC)
);
