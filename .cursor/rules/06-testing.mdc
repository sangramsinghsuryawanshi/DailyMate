---
description: Testing standards for DailyMate
globs:
  - backend/src/test/**/*.java
  - frontend/**/*.{test,spec}.{js,jsx,ts,tsx}
alwaysApply: false
---

# Testing Rules

Testing is part of implementation, not an afterthought.

## Backend

Use:

- JUnit 5
- Mockito
- Spring Boot Test
- MockMvc
- Testcontainers when integration testing requires real infrastructure

## Unit Tests

Test service business logic independently.

Cover:

- Success
- Validation
- Not found
- Duplicate/conflict
- Business rule failures
- Unexpected dependencies

## Controller Tests

Test:

- HTTP status
- Request validation
- Response structure
- Authorization
- Error responses

## Repository Tests

Use integration tests where database behavior matters.

## Security Tests

Test:

- Unauthenticated access
- Authenticated access
- Role-based access
- Invalid JWT
- Expired JWT where applicable

## Frontend

Test important user flows.

At minimum:

- Login
- Registration
- Protected routes
- Marketplace search
- Service details
- Error handling

## Test Quality

Do not write tests only to increase coverage percentage.

Tests should verify meaningful behavior.

Avoid excessive mocking of simple value objects.