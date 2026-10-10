# Iteration 14 — additional protection

The educational security iteration is complete. The API contract, database schema, tests and README describe the same behavior. The original learning plan remains a historical teaching guide; this document records the delivered behavior and acceptance criteria.

## Acceptance

| Requirement | Delivered behavior | Verification |
| --- | --- | --- |
| Block users | ADMIN status endpoint revokes all sessions; blocked users cannot login or refresh | Block/activate flow, forbidden USER request, missing target; old tokens remain rejected after activation |
| Change password | Verify current password, validate new password, store BCrypt hash and revoke sessions | Wrong current password leaves credentials intact; valid change rejects old password/tokens; UTF-8 byte limit |
| Logout | Authenticated POST revokes all sessions by incrementing user auth version and deleting refresh credentials | Two independent login sessions become unusable; fresh login succeeds |
| Audit | Persistent metadata for login, refresh, password, logout, status and throttling events | Committed successful events and failure events surviving rollback; ADMIN-only paginated access |
| Login attempts | Bounded, synchronized per-peer fixed windows; 429 and Retry-After before authentication | Expiry, capacity, parallel attempts, HTTP 429 and ignored forwarded headers |
| Sensitive endpoints | Password/logout require authentication; status/audit require ADMIN; account ownership unchanged | Anonymous 401, USER 403 and existing ownership regressions |
| CORS | Explicit localhost origin, no cookie credentials, exposed Retry-After | Allowed/disallowed preflight requests |
| CSRF | Explicit bearer-header authentication with stateless sessions; no cookie authentication | Cookie-only request receives 401; bearer POST succeeds without CSRF token |
| Headers | Spring Security content-type, frame, cache and HTTPS HSTS headers | Real security-chain response assertions |
| Secrets and logs | JWT key comes from environment; credentials omitted from audit and exception logging | Test configuration uses an isolated test key; runtime key validation; source review |

Global logout was chosen for a small, explainable implementation. It revokes all user sessions immediately without storing every access token. A request that passed authentication before revocation can still finish. Logout is not a per-device operation and requires a valid access token.

Successful security changes and their audit events share a transaction. Failed authentication/password checks write audit metadata in a separate transaction so rollback does not erase the failure. Anonymous login/refresh failures have no user UUID. Repeated throttle rejections are audited once per known peer window to avoid an event flood. Administrative audit access is read-only and capped at 100 entries per page.

## Verification

```bash
# Fast suite with migrated H2 schemas
mvn --batch-mode --no-transfer-progress verify

# Add PostgreSQL integration tests using a disposable bank_test database
export TEST_DATABASE_URL='jdbc:postgresql://localhost:5432/bank_test'
export TEST_DATABASE_USERNAME='postgres'
export TEST_DATABASE_PASSWORD='postgres'
mvn --batch-mode --no-transfer-progress -Ppostgres-integration verify
```

The regular suite has 58 tests. The PostgreSQL profile adds 11 integration tests: six committed lifecycle scenarios, three concurrent refresh/lifecycle races, transfer rollback and stale balance protection. Failsafe runs these integration classes separately from the H2 suite. GitHub Actions creates a PostgreSQL 16 service and runs both suites.

The database must be disposable: integration tests apply migrations and write/delete test users, credentials and audit events. Do not point the test profile at a development or production database. Test keys and credentials in this profile are exclusively for the disposable database.

## Upgrade

Migration `008-security-lifecycle.yaml` adds `users.auth_version`, the audit table and its date index. Existing schema changesets are retained. JWTs missing the new claim are rejected, and the migration deletes existing refresh credentials. All existing users must log in again after this upgrade; their passwords and accounts are retained.

Security commands acquire a user row lock before refresh rows, consistent with login and refresh issuance. User/account optimistic versions still prevent stale writes. PostgreSQL tests exercise simultaneous refresh attempts and refresh racing with logout/password change; they do not establish behavior under sustained load or every possible transfer race.

## Deployment limits

The limiter is local to one process, resets on restart and counts all attempts, including successful login. Shared NAT or proxy peers share a budget; untrusted forwarded IP headers are ignored. Multiple replicas need a gateway or shared limiter. Rate limits do not replace operational abuse monitoring.

Serve the API over HTTPS and provide signing/database secrets through a deployment secret store. HSTS is emitted only for HTTPS responses. A session application or cookie-authenticated JWT requires CSRF protection because browsers attach cookies automatically; CORS does not replace it. This API only supports explicit Authorization bearer headers.

Audit retention, expired-refresh cleanup, token-family replay tracking, transfer idempotency, broader concurrency/load tests and production deployment remain future work. These limits do not prevent completion of the documented educational iteration.
