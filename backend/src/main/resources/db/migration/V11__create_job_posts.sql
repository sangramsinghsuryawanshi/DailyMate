CREATE TABLE job_posts (
    id VARCHAR(36) NOT NULL,
    title VARCHAR(120) NOT NULL,
    category VARCHAR(80) NOT NULL,
    location VARCHAR(160) NOT NULL,
    type VARCHAR(60) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_job_posts_category ON job_posts(category);
