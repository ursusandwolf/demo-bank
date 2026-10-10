# Contributing

Use Java 21 and Maven 3.6.3 or later. Docker Compose is needed only for running PostgreSQL locally; tests use H2.

## Branches

- `main` contains the reviewed project snapshot displayed on GitHub.
- `dev` contains integrated development work.
- Create short-lived `feature/...`, `fix/...` or `docs/...` branches from `dev`, then open a pull request into `dev`.
- Promote verified changes from `dev` to `main`. Delete a completed branch only after its commits are included in `dev`.

## Before opening a pull request

1. Run `mvn --batch-mode --no-transfer-progress verify` with Java 21.
2. Add regression coverage when changing authentication, ownership checks, money handling or migrations.
3. Update the API documentation and changelog when observable behavior changes.
4. Explain the problem, resulting behavior and validation in the pull request description.

Keep credentials in environment variables. Never commit JWT signing keys, database dumps or IDE files. Add a new Liquibase changeset for schema changes; do not edit an already applied changeset.
