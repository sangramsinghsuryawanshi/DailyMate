CREATE TABLE service_providers (
    id CHAR(36) NOT NULL,
    name VARCHAR(80) NOT NULL,
    category VARCHAR(40) NOT NULL,
    description VARCHAR(500) NOT NULL,
    service_area VARCHAR(120) NOT NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(120) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_service_providers PRIMARY KEY (id)
);

CREATE INDEX idx_service_providers_category ON service_providers (category);
CREATE INDEX idx_service_providers_service_area ON service_providers (service_area);
