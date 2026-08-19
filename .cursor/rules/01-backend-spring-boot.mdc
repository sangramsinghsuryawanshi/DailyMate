---
description: Spring Boot backend development standards
globs:
  - backend/**/*.java
alwaysApply: false
---

# Backend Rules

## Java

Use Java 17.

Prefer modern Java features when they improve readability.

Avoid unnecessary advanced language features.

## Spring Boot

Follow standard Spring Boot conventions.

Use constructor injection.

Do NOT use field injection.

Preferred:

private final UserService userService;

public UserController(UserService userService) {
    this.userService = userService;
}

## Controller

Controllers should:

- Handle HTTP concerns
- Validate requests
- Delegate business logic
- Return DTOs

Controllers must NOT contain complex business logic.

Example responsibility:

HTTP Request
    ↓
Controller
    ↓
Service
    ↓
Repository

Service

Services contain business logic.

Keep services focused.

Avoid services becoming large God classes.

Repository

Repositories are responsible for persistence operations.

Use Spring Data JPA.

Do not put business logic in repositories.

DTO

Separate:

Request DTO
Response DTO
Entity

Never expose entities directly as REST API responses.

Mapping

Use MapStruct for complex or repeated entity/DTO mappings.

Avoid manually duplicating large mapping logic.

Transactions

Use @Transactional at the service layer where appropriate.

Keep transactions as short as practical.

Dependency Injection

Prefer constructor injection.

Use interfaces where abstraction provides real value.

Do not create interfaces solely because "every service needs an interface."

Exceptions

Create domain/application-specific exceptions where appropriate.

Use a global @RestControllerAdvice.

Return consistent error responses.

API Responses

Use appropriate HTTP status codes.

Examples:

GET successful → 200
POST created → 201
PUT/PATCH successful → 200
DELETE successful → 204
Validation failure → 400
Authentication failure → 401
Authorization failure → 403
Resource not found → 404
Conflict → 409
Unexpected server error → 500

Pagination

For collection APIs that can grow large, use pagination.

Prefer Spring Data Pageable.

Do not return unbounded database results.

Filtering

Filtering should be explicit and validated.

Avoid building unsafe dynamic SQL.

Use Spring Data Specifications or another appropriate abstraction when complex filtering is required.