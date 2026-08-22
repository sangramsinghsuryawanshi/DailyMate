---
description: MySQL and JPA database standards
globs:
  - backend/**/*.java
  - backend/**/*.sql
alwaysApply: false
---

# Database Rules

Use MySQL.

## Naming

Tables:

snake_case

Example:

users
service_providers
service_categories

Columns:

snake_case

Example:

created_at
updated_at
phone_number

Primary key:

id

Foreign keys:

<entity>_id

Example:

user_id
category_id

## JPA

Use explicit relationships.

Avoid unnecessary bidirectional relationships.

Be careful with:

@OneToMany
@ManyToMany

Prefer explicit join entities when a many-to-many relationship contains business data.

## IDs

Prefer Long IDs unless there is a strong reason to use another strategy.

Use database-generated IDs.

## Auditing

Entities should support:

created_at
updated_at

Use Spring Data auditing where appropriate.

## Constraints

Use database constraints for important data integrity rules.

Examples:

- NOT NULL
- UNIQUE
- FOREIGN KEY
- CHECK where supported

Application validation should complement database constraints.

## Performance

Avoid:

- N+1 queries
- Unbounded queries
- Excessive eager fetching

Prefer LAZY loading for relationships unless eager loading is justified.

Use indexes for frequently queried columns.

## Schema Changes

Never make destructive database changes casually.

For production:

- Use migrations.
- Review migration impact.
- Preserve backward compatibility where necessary.