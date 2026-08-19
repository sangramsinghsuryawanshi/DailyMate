CREATE TABLE local_events (
    id VARCHAR(36) NOT NULL,
    title VARCHAR(120) NOT NULL,
    category VARCHAR(80) NOT NULL,
    location VARCHAR(160) NOT NULL,
    event_date TIMESTAMP NOT NULL,
    description VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_local_events_event_date ON local_events(event_date);
