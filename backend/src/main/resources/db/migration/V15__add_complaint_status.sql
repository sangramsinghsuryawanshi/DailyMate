ALTER TABLE community_complaints ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'OPEN';
ALTER TABLE community_complaints ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
UPDATE community_complaints SET status = 'OPEN', updated_at = created_at WHERE status IS NULL OR status = '';
