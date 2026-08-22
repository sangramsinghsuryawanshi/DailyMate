-- V21: Add user ownership and composite indexes to emergency_contacts

ALTER TABLE emergency_contacts
  ADD COLUMN user_id CHAR(36) NULL;

ALTER TABLE emergency_contacts
  ADD CONSTRAINT fk_emergency_contacts_user
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX idx_emergency_contacts_user_id ON emergency_contacts(user_id);
CREATE INDEX idx_emergency_contacts_user_category ON emergency_contacts(user_id, category);
