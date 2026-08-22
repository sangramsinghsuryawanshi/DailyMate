ALTER TABLE notifications
  ADD COLUMN target_type VARCHAR(64),
  ADD COLUMN target_id VARCHAR(64),
  ADD COLUMN target_url VARCHAR(1024);
