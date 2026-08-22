---
description: Git and GitHub workflow for DailyMate
alwaysApply: true
---

# Git Workflow

Use GitHub for source control.

## Branches

main

Production-ready code.

develop

Integration branch when a multi-branch workflow is required.

feature/<name>

New functionality.

bugfix/<name>

Bug fixes.

hotfix/<name>

Urgent production fixes.

## Commits

Use meaningful commit messages.

Preferred format:

feat: add user registration
feat: add service marketplace search
fix: handle duplicate email
fix: resolve JWT validation issue
refactor: simplify authentication service
test: add login integration tests
docs: update architecture documentation
chore: update dependencies

## Pull Requests

Every meaningful feature should be reviewed before merging when collaboration is involved.

PR should describe:

- What changed
- Why it changed
- Testing performed
- Any database changes
- Any API changes

## Rules

Never commit:

- .env
- passwords
- API keys
- JWT secrets
- AWS credentials
- IDE-specific sensitive configuration

## Before Commit

Check:

1. Application builds.
2. Tests pass.
3. No secrets are committed.
4. Formatting is correct.
5. Unrelated files are not modified.

## Before Push

Review:

git status
git diff
git diff --staged

Keep commits focused.

Do not combine unrelated features into one commit.