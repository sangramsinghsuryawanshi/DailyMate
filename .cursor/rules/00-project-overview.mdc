---
description: Core architecture and development rules for the DailyMate project
alwaysApply: true
---

# DailyMate - Project Rules

## Project

DailyMate is a production-oriented SaaS platform designed to support everyday user needs.

Current MVP modules:

1. Authentication
2. User Profile
3. Local Service Marketplace

Future modules:

1. Medicine Reminder
2. Expense Tracker
3. Blood Donation
4. Lost & Found
5. Community Complaints
6. Emergency Contacts
7. Local Events
8. Grocery Price Comparison
9. AI Assistant
10. Notification Center
11. Admin Dashboard

## Architecture

Follow these principles:

- Clean Architecture
- Domain-Driven Design where appropriate
- SOLID
- DRY
- KISS
- Separation of Concerns
- Dependency Inversion
- Repository Pattern
- DTO Pattern
- Service Layer
- Global Exception Handling

## Important Rule

Do not introduce unnecessary complexity.

The MVP must remain practical for a single developer while allowing future scaling.

Prefer a modular monolith initially.

Do NOT introduce microservices unless there is a clear architectural reason.

The architecture must allow future extraction of modules into microservices.

## Technology

Backend:

- Java 17
- Spring Boot
- Spring Security
- JWT
- Spring Data JPA
- Hibernate
- MySQL
- Maven
- Bean Validation
- Lombok
- MapStruct
- OpenAPI/Swagger

Frontend:

- React
- React Router
- Axios
- React Query
- Tailwind CSS

Infrastructure:

- Docker
- GitHub
- GitHub Actions

Future:

- Redis
- RabbitMQ
- AWS S3
- AWS EC2
- Kubernetes

## Code Generation Rules

Before generating code:

1. Understand the existing architecture.
2. Inspect related files.
3. Reuse existing patterns.
4. Do not create duplicate functionality.
5. Do not modify unrelated files.
6. Explain important architectural changes.
7. Keep changes focused.

Never rewrite large portions of the application unnecessarily.

## Package Design

Organize backend code by business module rather than creating one giant global package.

Prefer:

feature/
    controller/
    service/
    repository/
    entity/
    dto/
    mapper/
    exception/

over large global packages such as:

controller/
service/
repository/
entity/

when the application grows.

## Scalability

Design APIs and modules so they can eventually support:

- Horizontal scaling
- Stateless backend instances
- Redis caching
- Async messaging
- Object storage
- Cloud deployment

Do not prematurely implement future infrastructure.

## API

Use versioned APIs:

/api/v1/...

Use RESTful naming.

Use DTOs for API contracts.

Never expose JPA entities directly from controllers.

## Database

Use MySQL.

Use migrations for production database changes.

Prefer Flyway when database migration tooling is introduced.

Do not rely on Hibernate auto schema generation in production.

## Security

Security is mandatory.

Never:

- Store plaintext passwords
- Return passwords in API responses
- Log passwords
- Log JWT tokens
- Hardcode secrets
- Commit credentials
- Trust client-provided roles

Passwords must be securely hashed using Spring Security's password encoder.

Authentication and authorization must be handled server-side.

## Error Handling

Use centralized exception handling.

Use consistent API error responses.

Do not expose stack traces or internal implementation details to clients.

## Validation

Validate all external input.

Use Jakarta Bean Validation where appropriate.

Validate:

- Request DTOs
- Query parameters
- Path variables when required
- Business rules in service/domain logic

## Logging

Use structured and meaningful logs.

Never log:

- Passwords
- JWT tokens
- Sensitive personal information
- Secrets

Use appropriate log levels:

ERROR
WARN
INFO
DEBUG

## Maintainability

Code should be:

- Readable
- Testable
- Small
- Cohesive
- Loosely coupled

Avoid:

- God classes
- God services
- Huge controllers
- Huge methods
- Deep nesting
- Duplicate logic
- Magic numbers
- Magic strings

## Comments

Do not add comments explaining obvious code.

Prefer clean code.

Use comments only when explaining:

- Non-obvious business rules
- Architectural decisions
- Workarounds
- External system behavior

## Existing Code

Before changing code, inspect the relevant implementation.

Do not assume a class, method, endpoint, or configuration does not exist.

Preserve existing functionality unless the requested change requires modification.

## Development Philosophy

Build DailyMate incrementally.

Architecture first.

Then implementation.

Then testing.

Then optimization.

Do not prematurely optimize.

Do not prematurely distribute the application into microservices.