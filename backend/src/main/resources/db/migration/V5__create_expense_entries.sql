CREATE TABLE expense_entries (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    category VARCHAR(40) NOT NULL,
    description VARCHAR(120) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    spent_on DATE NOT NULL,
    notes VARCHAR(500) DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_expense_entries_user_id (user_id),
    KEY idx_expense_entries_spent_on (spent_on)
);
