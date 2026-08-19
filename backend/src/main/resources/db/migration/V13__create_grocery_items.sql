CREATE TABLE grocery_items (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(120) NOT NULL,
    category VARCHAR(80) NOT NULL,
    store VARCHAR(80) NOT NULL,
    price DOUBLE NOT NULL,
    location VARCHAR(160) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_grocery_items_category ON grocery_items(category);
