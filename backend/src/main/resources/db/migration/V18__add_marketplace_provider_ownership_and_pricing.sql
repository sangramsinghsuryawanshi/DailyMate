-- V18: Add provider ownership and real hourly rate to service_providers

ALTER TABLE service_providers
  ADD COLUMN user_id CHAR(36) NULL,
  ADD COLUMN hourly_rate DECIMAL(12,2) NULL;

ALTER TABLE service_providers
  ADD CONSTRAINT fk_service_providers_user
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX idx_service_providers_user_id ON service_providers (user_id);
