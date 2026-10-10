# Code review — October 2026

Scope: all application sources, persistence mappings, migrations, existing tests, configuration and project documentation at `65e0b0d`. This report describes the corrective maintenance applied after consolidating the iteration branches.

## Findings and resolutions

| Severity | Finding | Resolution |
| --- | --- | --- |
| Blocker | Authentication registration referenced a missing DTO and undeclared dependencies; role enum was returned as `GrantedAuthority` | Reused the existing registration service and DTO; mapped roles to `SimpleGrantedAuthority` |
| Blocker | Duplicate registration-controller names, password encoder beans and user detail providers prevented consistent startup | Named the registration alias explicitly; retained a single encoder and user detail service |
| Blocker | Login, refresh and registration under `/api/auth` required prior authentication | Permitted the three public POST routes explicitly |
| Blocker | User fields and role column mapping did not match migrations; `account_locked` was absent; manually assigned generated IDs conflicted with versioning | Restored audit/status/version fields, mapped the `role` column, added the locking flag and used application-assigned UUIDs |
| Blocker | `TRANSFER_OUT` exceeded the transaction type column length | Widened the column in a new additive Liquibase changeset |
| Major | Malformed/expired JWTs could escape the security filter as server errors; JWT and refresh ignored user status | Added authentication error handling and status checks, including blocked/locked/expired users |
| Major | Refresh tokens were reusable plaintext credentials; documentation claimed rotation that did not exist | Store only SHA-256 hashes; rotate under a database row lock and reject reuse |
| Major | JWT signing key was committed in configuration | Require a new key from `JWT_SECRET` and validate it at startup |
| Major | Amount precision and length limits did not match database constraints | Added decimal precision, UTF-8 password byte limits and DTO length checks; enforce positive amounts in the transfer service |
| Major | Optimistic conflicts and authentication exceptions became 500; generic errors exposed exception details | Added appropriate 400/401/403/409 handling and generic 500 messages |
| Major | Old tests assumed HTTP Basic and schema recreation instead of testing the current authentication/persistence model | Switched authentication scenarios to JWT login/bearer tokens, validated migrated schemas and isolated fixtures |
| Minor | Immutable role collections failed during entity merge | Copy role collections into mutable sets when constructing or assigning them |
| Minor | Account DTO creation depended on Open Session in View | Disable it and use explicit read transactions for account queries |
| Minor | Demo users were seeded by default, database data was ephemeral and repository docs described obsolete branches/features | Scope seed data to the demo context, persist local data and rewrite docs around actual behavior |

The security, registration, ownership, transfer and persistence regressions are covered by the test suite. Transfer persistence tests deliberately fail after writing the first history record and flushing balance changes, then confirm transaction rollback. A separate stale-update test confirms that an account version prevents overwriting a committed balance.

## Verification

Run the complete build using Java 21:

```bash
mvn --batch-mode --no-transfer-progress verify
```

The suite runs Liquibase migrations on H2 with Hibernate schema validation. GitHub Actions repeats the build for both maintained branches. PostgreSQL-specific integration and concurrency testing has not been completed in this maintenance pass; Docker was unavailable in the local environment.

Local verification on 2026-10-10 completed successfully: **49 tests, zero failures, zero errors, zero skipped**. Maven `verify` produced the executable application JAR with Java 21.

Dependency versions were updated within Spring Boot 3 to reduce migration scope. Sources: [Spring Boot 3.5 documentation](https://docs.spring.io/spring-boot/3.5/reference/using/index.html) and [JJWT releases](https://github.com/jwtk/jjwt/releases). This update does not establish that every dependency is free of vulnerabilities; Dependabot is configured to propose further updates.

## Upgrade impact

- Supply a private Base64 `JWT_SECRET`; do not use the old repository key.
- The new migration deletes existing refresh credentials before introducing hashed storage. Users need to log in again.
- Existing Liquibase changesets remain intact; schema fixes use `007-review-fixes.yaml`.
- Default databases no longer seed the historical demo user. Existing users are retained.
- Multiple login sessions have independent refresh tokens; family revocation and logout are not implemented.

## Remaining scope

Production hardening requires PostgreSQL concurrency verification, transfer idempotency, refresh-family revocation, rate limiting, security audit logging and deployment work. These are tracked in [next steps](PROJECT_CONTEXT.md#next-steps). The project is presented as a portfolio backend with its limitations stated explicitly.
