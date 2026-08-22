-- V22: Add contributor ownership, quantity unit, and timestamps to grocery_items

ALTER TABLE grocery_items
  ADD COLUMN user_id CHAR(36) NULL,
  ADD COLUMN unit VARCHAR(40) NOT NULL DEFAULT '1 unit',
  ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE grocery_items
  ADD CONSTRAINT fk_grocery_items_user
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX idx_grocery_items_user_id ON grocery_items(user_id);
CREATE INDEX idx_grocery_items_name ON grocery_items(name);
CREATE INDEX idx_grocery_items_user_category ON grocery_items(user_id, category);
