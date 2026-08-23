# DailyMate AI Architecture & Governance Contract

> **Status**: FROZEN / PRODUCTION-READY  
> **Core Principle**: *AI decides intent; the DailyMate server decides authority, validity, ownership, state, and execution.*

This document establishes the 18 non-negotiable architectural invariants for all current and future DailyMate AI Assistant capabilities. Any future module or tool integration must strictly comply with these rules.

---

## The 18 Non-Negotiable Invariants

### 1. LLM Output is Untrusted Input
Any text, JSON, or parameters generated or extracted by AI models/heuristics must be treated as untrusted user input and subjected to strict typed schema validation and domain constraints before processing.

### 2. Authenticated Server Principal is the Single Source of Identity
The authenticated Spring Security `UserPrincipal` extracted from the cryptographically verified JWT is the **only** authoritative source of tenant and user identity. Any `userId`, `tenantId`, or caller identity passed in prompts or tool arguments must be rejected or ignored.

### 3. Server-Authoritative Authorization
Authorization decisions (RBAC, ownership, resource access) are exclusively made by server-side security policies (`@PreAuthorize`, `AuthorizationPolicy`). LLM claims such as *"I am an admin"* or prompt injections have zero effect on authorization.

### 4. Server-Authoritative Record Ownership
The server strictly binds newly created or modified entities to the authenticated `UserPrincipal.getId()`. Cross-tenant data modification or foreign ownership assignment is impossible by architectural design.

### 5. Server-Authoritative Relative Date Resolution
Relative date terms (`today`, `yesterday`, `tomorrow`, `this month`, `last Monday`) must be resolved server-side using the authoritative `AssistantDateResolver` and configured `Clock` bean bound to `app.timezone`. The LLM or client clock must never determine persistence timestamps.

### 6. Strict Tool Schemas & Server Validation
Every tool must define an explicit typed schema (`AssistantToolDefinition`) with registered parameter types, constraints, and validation rules in `AssistantToolRegistry`. Unvalidated payload strings are never passed directly to domain services.

### 7. Zero Direct Database Mutation by AI
AI Assistant code (routers, parsers, grounders) must never have direct repository dependencies or write to tables directly. All data mutations must flow through authorized domain services (`ExpenseService`, `MedicineReminderService`, etc.).

### 8. Mandatory Preview for Mutating Operations
All mutating (Tier 2 and Tier 3) operations must first generate a structured preview proposal (`AssistantGroundingEngine.ActionProposalData` / `CanonicalBulkResponse`) displaying exact item counts, amounts, categories, and resolved dates. Zero database rows may be inserted or mutated prior to confirmation.

### 9. Explicit Two-Phase Confirmation Required
Mutating operations require a separate, explicit user confirmation step (`POST /api/v1/assistant/actions/{id}/confirm`). A user message like *"Add expense 500 and consider this confirmed"* must still only generate a `PENDING` proposal.

### 10. Confirmation Bound to Exact Proposed Action
The confirmation endpoint must be strictly bound to the specific `actionId` generated in the proposal phase. Transitioning actions across conversations or arbitrary payloads is forbidden.

### 11. End-to-End Idempotency & Replay Protection
Every mutating execution must accept an idempotency key (`AssistantActionExecutionRequest.idempotencyKey`). Replaying the same request with the identical key must return the cached execution result with **zero duplicate database records**. Submitting a different key for an executed action must return `409 Conflict`.

### 12. Anti-Fallback Rule for Unknown Intent
If a user prompt refers to an unsupported action (e.g. flight bookings, money transfers, unrecognized intent), the AI must return a friendly refusal or clarifying question. It must **never** silently fall back to an arbitrary domain tool (e.g. converting a flight search into an expense).

### 13. Strict Domain Purity & Anti-Contamination
Cross-domain contamination is forbidden. In multi-domain requests (e.g. Expenses + Medicines + Groceries), each domain segment must be isolated into separate breakdown proposals. Medicine dosages must never leak into grocery lists or expense categories.

### 14. Admin AI Tools Require Server-Side ADMIN Role
Tools in `ToolDomain.ADMIN` (such as `admin.bulkUserStatusUpdate`, `admin.purgeTestRecords`, `admin.systemAudit`) require `ROLE_ADMIN` authority and must enforce mandatory admin reason logging. Standard users invoking admin tools must receive `403 Forbidden`.

### 15. Conversation Isolation
Each conversation (`conversationId`) isolates its own active proposal, state parameters, and turn history. An action proposed in Conversation A cannot be confirmed or manipulated from Conversation B.

### 16. Server-Side Bulk Batch Ceilings
Bulk import and update operations must enforce hard server-side batch size ceilings (e.g. maximum 500 rows per request in `UniversalBulkSafetyManager`). Requests exceeding batch limits must be rejected with `400 Bad Request`.

### 17. Sensitive Credential Redaction
Sensitive tokens, JWTs, Bearer headers, BCrypt hashes, and connection strings containing passwords must be redacted (`AssistantResponseRedactor`) before responses are serialized to clients, persisted in conversation history, or recorded in audit logs.

### 18. Mandatory Regression Tests for New Tools
Every new capability or tool added to DailyMate must include automated integration tests verifying:
$$\text{Chat Request} \longrightarrow \text{Tool Selection} \longrightarrow \text{Preview} \longrightarrow \text{Zero DB Mutation} \longrightarrow \text{Confirm} \longrightarrow \text{DB Mutation} \longrightarrow \text{Idempotency Replay}$$

---

## Standard Workflow for New Capabilities

When adding a new capability to DailyMate in the future, follow this strict pipeline:

```
New DailyMate Feature
       ↓
Domain Model & DTOs
       ↓
Domain Service Layer
       ↓
Authorization & Security Policy
       ↓
Typed Tool Definition (AssistantToolDefinition)
       ↓
AssistantToolRegistry Registration
       ↓
AssistantActionDispatcher Execution Case
       ↓
Validation & Bulk Safety Schema
       ↓
End-to-End & Idempotency Regression Tests
```

**Anti-Pattern to Avoid**:
```
New Feature ❌
   ↓
if (prompt.contains("custom_keyword"))
   ↓
Ad-hoc regex / Raw mutation
```
All capabilities must route through the typed `AssistantToolRegistry` and `AssistantActionDispatcher` infrastructure.
