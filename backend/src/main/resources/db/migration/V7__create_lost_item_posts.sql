CREATE TABLE lost_item_posts (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    title VARCHAR(120) NOT NULL,
    item_type VARCHAR(80) NOT NULL,
    location VARCHAR(160) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    contact_name VARCHAR(80) NOT NULL,
    contact_phone VARCHAR(80) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_lost_item_posts_user_id (user_id),
    KEY idx_lost_item_posts_created_at (created_at)
);
