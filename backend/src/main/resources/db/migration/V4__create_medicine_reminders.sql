CREATE TABLE medicine_reminders (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    name VARCHAR(80) NOT NULL,
    dosage VARCHAR(80) NOT NULL,
    frequency VARCHAR(40) NOT NULL,
    remind_at TIME NOT NULL,
    notes VARCHAR(500) DEFAULT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_medicine_reminders_user_id (user_id),
    KEY idx_medicine_reminders_active (active)
);
