---
description: Security rules for DailyMate
globs:
  - backend/**/*.java
  - backend/**/*.yml
  - backend/**/*.yaml
  - backend/**/*.properties
alwaysApply: false
---

# Security Rules

## Authentication

Use Spring Security.

Passwords must be hashed.

Never store plaintext passwords.

## JWT

JWT should contain only necessary claims.

Never put passwords, secrets, or sensitive information inside JWT claims.

Never log JWT tokens.

## Authorization

Use role-based authorization.

Example roles:

USER
SERVICE_PROVIDER
ADMIN

Do not trust roles sent by the frontend.

Authorization must be enforced on the backend.

## Secrets

Never hardcode:

- Database passwords
- JWT secrets
- AWS credentials
- API keys
- OAuth secrets

Use environment variables or a proper secrets manager.

## CORS

Configure CORS explicitly.

Do not use unrestricted origins in production.

## Validation

Validate all externally supplied data.

## SQL

Never construct SQL queries using unsafe string concatenation.

Use JPA parameters or prepared statements.

## Logging

Never log:

- Passwords
- Tokens
- Authentication headers
- Secrets

## Dependencies

Prefer stable versions.

Keep dependencies updated.

Do not add dependencies without a clear reason.

## Security Changes

Any change involving:

- Authentication
- Authorization
- JWT
- Passwords
- Roles
- Permissions
- CORS
- CSRF
- Sessions

must be treated as security-sensitive and reviewed carefully.