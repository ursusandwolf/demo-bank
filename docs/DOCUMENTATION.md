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

## Profiles and roles

| Endpoint | Behavior |
| --- | --- |
| `GET /api/public/health` | Public liveness response `{"status":"UP"}`; it does not probe database availability |
| `GET /api/private/hello` | Example endpoint for `USER` or `ADMIN` |
| `GET /api/users/me` | Current user's ID, email and names; never password or token fields |
| `GET /api/users/count` | User count for `USER` or `ADMIN` |
| `GET /api/admin/users` | User DTOs for `ADMIN` only |

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
| 404 | Account details unavailable to the caller; this response has no error body |
| 409 | Duplicate registration, data constraint conflict or optimistic locking conflict |
| 500 | Unexpected server failure; internal exception details stay in server logs |

Validation codes include `VALIDATION_ERROR`; registration uses `USER_ALREADY_EXISTS`; optimistic conflicts use `CONCURRENT_MODIFICATION`. Trace IDs correlate application errors with logs. The health endpoint is public, CORS permits `http://localhost:3000`, and sessions are stateless. CSRF is disabled because credentials are explicit bearer headers rather than browser session cookies.
