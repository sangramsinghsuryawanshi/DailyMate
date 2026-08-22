-- V23: Add owner user_id, compensation salary, company/contact details, status lifecycle, and updated_at to job_posts

ALTER TABLE job_posts
    ADD COLUMN user_id CHAR(36) NULL,
    ADD COLUMN salary DECIMAL(12, 2) NULL,
    ADD COLUMN company_name VARCHAR(120) NULL,
    ADD COLUMN contact_phone VARCHAR(20) NULL,
    ADD COLUMN contact_email VARCHAR(120) NULL,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE job_posts
    ADD CONSTRAINT fk_job_posts_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE;

CREATE INDEX idx_job_posts_user_id ON job_posts(user_id);
CREATE INDEX idx_job_posts_status ON job_posts(status);
CREATE INDEX idx_job_posts_user_status ON job_posts(user_id, status);
CREATE INDEX idx_job_posts_category_status ON job_posts(category, status);
