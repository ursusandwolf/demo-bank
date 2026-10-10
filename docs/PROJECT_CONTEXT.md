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

## Iteration status

The original learning plan uses iterations 0–14 rather than formal sprint dates. The foundational user, account, transfer and JWT features from iterations 0–13 are implemented. Iteration 14 is now complete for the documented educational API: administrator blocking, password change, global logout, persistent security audit, login throttling, sensitive-route authorization, CORS/CSRF decisions, security headers and external signing secrets. See [acceptance and limits](ITERATION_14.md).

The backend is at the end of the planned educational security iteration, with regression coverage and automated delivery checks. Production readiness is a separate milestone.

## Next steps

These are planned improvements, not implemented capabilities:

- Broader PostgreSQL concurrent-transfer load tests; CI now includes migration validation, transfer rollback/stale updates and simultaneous refresh/lifecycle requests.
- Transfer idempotency keys and stronger ledger reconciliation.
- Refresh-token family replay tracking and expiry cleanup; global logout and revocation are implemented.
- Distributed login limits, audit retention and deployment secret management; a bounded local limiter and database audit are implemented.
- Paginated administrative user listing and an explicit account provisioning workflow.
- OpenAPI documentation and a client application.
- Migration to a supported Spring Boot 4 release, with compatibility testing.

The application has no production deployment or external payment integration. CI checks both H2 and selected PostgreSQL persistence/concurrency scenarios; this is not a load test or comprehensive security audit. Dependency updates are tested by CI.
