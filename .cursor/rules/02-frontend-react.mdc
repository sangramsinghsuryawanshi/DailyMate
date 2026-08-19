
### `02-frontend-react.mdc`

```text
---
description: React frontend architecture and coding standards
globs:
  - frontend/**/*.{js,jsx,ts,tsx}
alwaysApply: false
---

# React Rules

Use React with functional components.

Avoid class components.

## Structure

Organize frontend primarily by feature.

Example:

src/
├── app/
├── features/
│   ├── auth/
│   ├── profile/
│   └── marketplace/
├── components/
├── layouts/
├── hooks/
├── services/
├── api/
├── utils/
└── assets/

## API

Use Axios for HTTP communication.

Do not call Axios directly from every component.

Centralize API configuration.

## Server State

Use React Query for:

- API data
- Caching
- Loading state
- Error state
- Mutations
- Refetching

Do not duplicate server state unnecessarily in local React state.

## Routing

Use React Router.

Protect authenticated routes.

Separate public and protected routes.

## Components

Components should have one clear responsibility.

Avoid very large components.

Extract reusable UI components.

## Styling

Use Tailwind CSS.

Avoid unnecessary custom CSS.

Keep styling consistent.

## Forms

Validate user input on the frontend for UX.

Remember that backend validation is authoritative.

Never rely only on frontend validation for security.

## Authentication

Do not expose sensitive authentication information unnecessarily.

Handle:

- Login
- Logout
- Authentication state
- Unauthorized responses
- Protected routes

through a centralized authentication strategy.

## UX

Every API-driven screen should properly handle:

- Loading
- Success
- Empty state
- Error state

Do not leave users with blank screens during API calls.