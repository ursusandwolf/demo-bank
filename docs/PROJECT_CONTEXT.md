# Project status

Demo Bank is a portfolio backend focused on Spring Security and transactional persistence. It is maintained as an educational project, with development integrated in `dev` and reviewed snapshots published in `main`.

## October 2026 maintenance

- Consolidated the iteration branches without losing their commits.
- Fixed compilation, duplicate Spring beans and user entity/schema inconsistencies.
- Made login, registration and refresh reachable without an access token.
- Added JWT validation, account status enforcement, refresh rotation and hashed refresh-token storage.
- Tightened money validation and made conflict/error responses explicit.
- Updated Spring Boot to 3.5.16 and JJWT to 0.13.0.
- Updated tests to match JWT authentication and validate Liquibase schemas.
- Added GitHub Actions verification and weekly Dependabot updates.
- Replaced outdated documentation with runnable examples and an explicit branch workflow.

## Next steps

These are planned improvements, not implemented capabilities:

- PostgreSQL integration tests for migrations, concurrent transfers and simultaneous refresh requests.
- Transfer idempotency keys and stronger ledger reconciliation.
- Refresh-token family revocation, logout and expiry cleanup.
- Login rate limiting, security event auditing and deployment secret management.
- Paginated administrative user listing and an explicit account provisioning workflow.
- OpenAPI documentation and a client application.
- Migration to a supported Spring Boot 4 release, with compatibility testing.

The application has no production deployment or external payment integration. H2-based tests do not establish PostgreSQL concurrency behavior. Dependency updates are tested by CI; no comprehensive security audit is claimed.
