CREATE TABLE emergency_contacts (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(120) NOT NULL,
    category VARCHAR(60) NOT NULL,
    phone VARCHAR(40) NOT NULL,
    location VARCHAR(160) NOT NULL,
    description VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_emergency_contacts_category ON emergency_contacts(category);
