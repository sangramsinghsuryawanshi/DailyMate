-- V20: Add organizer ownership, status lifecycle, and timestamps to local_events

ALTER TABLE local_events
  ADD COLUMN user_id CHAR(36) NULL,
  ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE local_events
  ADD CONSTRAINT fk_local_events_user
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX idx_local_events_user_id ON local_events(user_id);
CREATE INDEX idx_local_events_category ON local_events(category);
CREATE INDEX idx_local_events_status ON local_events(status);
