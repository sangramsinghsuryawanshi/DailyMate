CREATE TABLE donation_centers (
    id CHAR(36) NOT NULL,
    name VARCHAR(120) NOT NULL,
    location VARCHAR(160) NOT NULL,
    contact VARCHAR(40) NOT NULL,
    description VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_donation_centers_name (name)
);
