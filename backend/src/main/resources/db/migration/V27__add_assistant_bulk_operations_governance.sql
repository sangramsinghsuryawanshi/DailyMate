ALTER TABLE assistant_bulk_operations
    ADD COLUMN confirmation_phrase VARCHAR(120),
    ADD COLUMN admin_reason VARCHAR(500),
    ADD COLUMN target_snapshot_json LONGTEXT,
    ADD COLUMN dry_run BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN expires_at TIMESTAMP NULL;
