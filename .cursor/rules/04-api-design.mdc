---
description: REST API standards for DailyMate
alwaysApply: true
---

# API Design

Base URL:

/api/v1

## Naming

Use nouns rather than verbs.

Good:

GET /api/v1/users
GET /api/v1/services
POST /api/v1/auth/login

Avoid:

GET /api/v1/getUsers
POST /api/v1/createUser

## Authentication

Authentication endpoints:

POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout

## Marketplace

Example:

GET /api/v1/services
GET /api/v1/services/{id}
POST /api/v1/services
PUT /api/v1/services/{id}
DELETE /api/v1/services/{id}

## Pagination

Use:

?page=0&size=20

## Sorting

Use:

?sort=name,asc

## Filtering

Use meaningful query parameters.

Example:

?category=electrician&location=pune

Avoid extremely complex query syntax in the MVP.

## Response

Responses must have predictable structures.

Collections should support pagination metadata where appropriate.

## Errors

Use a consistent structure such as:

{
  "timestamp": "...",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "...",
  "path": "...",
  "errors": []
}

Never expose stack traces.

## HTTP

Use HTTP semantics correctly.

Do not return 200 for every situation.

## Backward Compatibility

Avoid breaking existing API contracts.

If a breaking change is required, introduce a new API version.