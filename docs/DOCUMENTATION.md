# API documentation

The application listens on `http://localhost:8080`. Authentication uses `Authorization: Bearer <accessToken>`. HTTP Basic is disabled. The default database configuration is in `src/main/resources/application.yml`.

## Registration and authentication

`POST /api/auth/register` and the compatibility alias `POST /api/public/register` accept:

```json
{"email":"alice@example.com","password":"password123","firstName":"Alice","lastName":"Example"}
```

Successful registration returns 201 with the user's ID, email and names. Duplicate email returns 409. Invalid input returns 400. Passwords need at least eight characters and cannot exceed 72 UTF-8 bytes. Clients cannot assign roles: registration always grants `USER`.

`POST /api/auth/login` accepts JSON containing `email` and `password`. It returns:

```json
{"accessToken":"<signed-jwt>","refreshToken":"<opaque-token>","tokenType":"Bearer","expiresIn":900}
```

Invalid credentials and blocked, locked or expired users receive 401. Access tokens last 15 minutes; refresh tokens last seven days.

`POST /api/auth/refresh` accepts the refresh token directly as a `text/plain` body. A successful refresh returns the same response shape with a replacement refresh token. The old refresh token cannot be reused. Tokens are stored as SHA-256 hashes, and rotation locks the matching database row. Multiple logins create independent refresh tokens. Expired tokens are rejected; a scheduled cleanup job is not implemented.

## Password changes, logout and blocking

`POST /api/auth/change-password` requires a bearer token and JSON:

```json
{"currentPassword":"password123","newPassword":"aDifferentPassword123"}
```

Success returns 204. The new password must differ from the current password, contain at least eight characters and fit 72 UTF-8 bytes. Invalid input returns 400; an incorrect current password returns 401. Rejected requests leave the password and sessions unchanged.

`POST /api/auth/logout` requires a bearer token, accepts no body and returns 204. **Logout is global for that user:** all access and refresh tokens from all login sessions are revoked. The token used for logout receives 401 on subsequent requests. Log in again to obtain new credentials. Password changes also revoke all sessions.

`PUT /api/admin/users/{id}/status` accepts `{"status":"BLOCKED"}` or `{"status":"ACTIVE"}` and requires `ADMIN`. Success returns 204, an unknown ID returns 404 and an invalid status returns 400. Each status command revokes all sessions, including activation. Unblocking never restores old tokens. An administrator may block their own account; another administrator must restore it.

JWTs carry an `authVersion` claim checked against the current database user on every bearer request. Lifecycle commands increment this version and delete refresh credentials in the same transaction. Requests already authorized before a revocation may finish; the next authentication check rejects the old token. Login/refresh issuance and lifecycle changes lock the user before refresh rows. PostgreSQL concurrency verification remains on the roadmap.

## Login limits and security audit

Login accepts up to `LOGIN_MAX_ATTEMPTS` requests per socket peer IP in a fixed `LOGIN_WINDOW_SECONDS` window (defaults: 10 and 60). All login requests count, including successful and malformed requests. Further attempts receive 429 with `LOGIN_RATE_LIMITED`, a trace ID and `Retry-After` seconds. A successful login does not reset the counter. The filter runs before credential verification. `X-Forwarded-For` is ignored; behind a reverse proxy the peer may be the proxy, and clients sharing that peer share the budget.

The limiter is per process and resets on restart. Storage is bounded by `LOGIN_MAX_CLIENTS` (default 10000); new peers are rejected when all buckets are active. Expired buckets can be reused. Multiple application replicas need a shared gateway or distributed limiter before deployment.

`GET /api/admin/security-events?page=0&size=20` returns a page of `id`, `type`, `actorId`, `targetId` and `occurredAt`, newest first. Page must be non-negative; size is 1–100. Events cover successful/failed login and refresh, logout, password changes and failures, blocking/activation, and the first throttled login per known peer window. Capacity rejections are not individually audited. Failed login/refresh attempts have no actor ID; successful operations refer to user UUIDs. Failed attempts survive transaction rollback; successful lifecycle events commit with their changes.

The audit stores no email, password, token, IP address or request body. Exception logging records class and trace ID rather than potentially credential-bearing messages or stack traces. Audit access is read-only through the API; database administrators must define retention and access policies for a deployment. Token-family replay tracking and automatic audit/refresh cleanup remain future work.

## Profiles and roles

| Endpoint | Behavior |
| --- | --- |
| `GET /api/public/health` | Public liveness response `{"status":"UP"}`; it does not probe database availability |
| `GET /api/private/hello` | Example endpoint for `USER` or `ADMIN` |
| `GET /api/users/me` | Current user's ID, email and names; never password or token fields |
| `GET /api/users/count` | User count for `USER` or `ADMIN` |
| `GET /api/admin/users` | User DTOs for `ADMIN` only |
| `PUT /api/admin/users/{id}/status` | Administrative block/activation with session revocation |
| `GET /api/admin/security-events` | Paginated security events for `ADMIN` only |

Role authorities are `USER` and `ADMIN`, without a `ROLE_` prefix. JWT requests reload the user from the database, so account status changes take effect on subsequent requests. An admin has no exemption from account ownership checks.

## Accounts and history

`GET /api/accounts` lists the caller's accounts. `GET /api/accounts/{number}` returns an account DTO or 404, including when the account belongs to somebody else.

`GET /api/accounts/{number}/transactions?page=0&size=20&sort=createdAt,desc` returns a Spring Data page. Only the owner can see history; unknown and foreign accounts produce an empty page. Supply an explicit sort when ordering matters.

Registration does not provision bank accounts. There is no public account-opening, funding or role-assignment endpoint.

## Demo accounts

For a local transfer walkthrough, register `alice@example.com` and `bob@example.com` using the registration API, then load the fixtures:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"bob@example.com","password":"password123","firstName":"Bob","lastName":"Example"}'
docker compose exec -T db psql -U postgres -d bank < docs/demo-accounts.sql
```

PowerShell alternative for the SQL command:

```powershell
Get-Content docs/demo-accounts.sql -Raw | docker compose exec -T db psql -U postgres -d bank
```

The fixture creates `ALICE-USD` with 1000.00 USD and `BOB-USD` with 500.00 USD, only when their users already exist. Re-running it preserves existing balances. These fixtures are strictly for local development, and initial funding does not create history records.

## Transfers

With Alice's access token:

```bash
curl -i -X POST http://localhost:8080/api/transfers \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"fromAccountNumber":"ALICE-USD","toAccountNumber":"BOB-USD","amount":25.00,"description":"Lunch"}'
```

Success returns 200 with an empty body. Both balances and the `TRANSFER_OUT` / `TRANSFER_IN` records are written in one transaction.

The source must belong to the caller. Both accounts must be active, distinct and use the same currency. Amounts must be positive, fit `DECIMAL(19,2)` and contain at most two decimal places. Descriptions are limited to 255 characters. Insufficient funds, missing accounts and business validation failures return 400. Concurrent balance modifications return 409; the client can retry after re-reading account state. Transfers have no idempotency key, so do not blindly retry a request whose outcome is unknown.

## Errors

Security and application errors contain `timestamp`, `status`, `code`, `message`, `path` and `traceId`:

```json
{
  "timestamp":"2026-10-10T07:00:00Z",
  "status":401,
  "code":"UNAUTHORIZED",
  "message":"Full authentication is required to access this resource",
  "path":"/api/users/me",
  "traceId":"b5fddf37-e801-4de4-b0ca-4fe2a480bd08"
}
```

| Status | Meaning |
| --- | --- |
| 400 | Invalid JSON, validation failure or invalid transfer |
| 401 | Missing/invalid/expired authentication, invalid refresh token or disallowed user status |
| 403 | Insufficient role authority |
| 404 | Account details unavailable (empty body), or administrative user ID absent (JSON error) |
| 409 | Duplicate registration, data constraint conflict or optimistic locking conflict |
| 429 | Login budget exhausted; retry after the seconds in `Retry-After` |
| 500 | Unexpected server failure; logs record exception class and trace ID |

Validation codes include `VALIDATION_ERROR`; registration uses `USER_ALREADY_EXISTS`; optimistic conflicts use `CONCURRENT_MODIFICATION`. Trace IDs correlate application errors with logs; security audit events have separate IDs. The health endpoint is public. CORS permits `http://localhost:3000`, exposes `Retry-After` and does not allow credentialed cookie requests. Sessions are stateless and the API creates no session cookie.

CSRF is disabled because this API authenticates only explicit bearer headers, not ambient browser credentials. A session application or JWT stored in an authentication cookie needs CSRF protection: a browser attaches those cookies automatically to requests, including requests initiated by other sites. CORS alone does not replace CSRF protection. Cookie-based authentication is not supported here.

Spring Security supplies `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, cache-prevention headers and HSTS on HTTPS responses. Serve deployments over HTTPS. `JWT_SECRET` is required from the environment; keep signing keys and database passwords outside the repository and use a deployment secret store. Request-body and authorization-header logging must remain disabled.
