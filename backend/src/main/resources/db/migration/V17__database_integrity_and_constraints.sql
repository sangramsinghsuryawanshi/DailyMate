-- V17: Enforce foreign key constraints, correct monetary types, and add performance indexes

-- 1. Align assistant_conversations.user_id type with users.id
ALTER TABLE assistant_conversations
  MODIFY COLUMN user_id CHAR(36) NOT NULL;

-- 2. Add foreign keys to user-owned domain tables
ALTER TABLE medicine_reminders
  ADD CONSTRAINT fk_medicine_reminders_user
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE expense_entries
  ADD CONSTRAINT fk_expense_entries_user
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE lost_item_posts
  ADD CONSTRAINT fk_lost_item_posts_user
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE assistant_conversations
  ADD CONSTRAINT fk_assistant_conversations_user
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE notifications
  ADD CONSTRAINT fk_notifications_user
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 3. Standardize floating-point monetary column to decimal
ALTER TABLE grocery_items
  MODIFY COLUMN price DECIMAL(12,2) NOT NULL;

-- 4. Add performance index for notification status queries
ALTER TABLE notifications
  ADD INDEX idx_notifications_user_unread (user_id, is_read);
