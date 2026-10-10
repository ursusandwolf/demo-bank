# Changelog

## Iteration 14 completion — 2026-10-10

- Added authenticated password changes and global logout with immediate access/refresh revocation.
- Added ADMIN user block/activation and paginated security audit endpoints.
- Persisted security events without passwords, tokens, request bodies or raw email addresses.
- Added bounded login throttling with 429/Retry-After, verified CORS/headers and disabled browser logout.
- Added auth-version/audit schema migration; existing users must log in again after upgrading.
- Extended verification to 58 regular tests and 11 PostgreSQL integration scenarios in CI.
- Reconciled the README, API contract, project status and iteration acceptance documentation.

Historical iteration completion notes below describe earlier snapshots; the current delivered security contract is recorded in [ITERATION_14.md](ITERATION_14.md).

## Maintenance — 2026-10-10

### Fixed
- Restored compilation and removed duplicate security/registration bean definitions.
- Reconciled user authorities, audit fields and locking flags with the Liquibase schema.
- Enabled public JWT login/refresh/registration, checked account status on token requests and handled malformed/expired tokens as 401.
- Implemented refresh-token rotation with row locking and SHA-256 storage.
- Added the missing `account_locked` column, widened transaction types and constrained refresh-token hashes in an additive migration.
- Added money precision and input length checks, optimistic conflict responses and safe server error messages.
- Updated tests to JWT authentication, schema validation and transaction isolation; added token lifecycle, rollback and stale-update regression coverage.

### Changed
- Updated Spring Boot from 3.3.0 to 3.5.16 and JJWT from 0.12.5 to 0.13.0.
- Required `JWT_SECRET` from the environment; the previous checked-in key must not be reused.
- Disabled Open Session in View and explicitly scoped account DTO queries to read transactions.
- Restricted the historical test-user seed to the `demo` Liquibase context.
- Persisted local PostgreSQL data in a volume and bound its port to the loopback interface.
- Consolidated development into `dev`; reviewed snapshots use `main`.

### Added
- GitHub Actions Maven verification, test-report artifacts and weekly Dependabot updates.
- A portfolio README, API walkthrough, local account fixtures, contribution guide and review report.

### Upgrade notes
- Set a new private Base64 `JWT_SECRET` before startup.
- The new migration invalidates existing refresh tokens. Users must log in again.
- Existing Liquibase changesets are preserved; apply the new changeset normally.
- New databases do not create the historical demo user unless the `demo` context is selected.

## [0.0.1] - 2026-06-09
### Added
- Project skeleton (Iteration 0).
- `HealthController` with `/api/public/health`.
- `GlobalExceptionHandler` and `ErrorResponse`.
- Liquibase master changelog.
- Maven configuration with Java 21 and Spring Boot 3.3.0.
- PostgreSQL and H2 (test) configurations.

## [0.1.0] - 2026-06-09
### Added
- Spring Security integration.
- `SecurityConfig` with `SecurityFilterChain`.
- `/api/private/hello` protected endpoint.
- In-memory user configuration for initial testing.
- Security tests with `MockMvc` and `@WithMockUser`.

## [0.2.0] - 2026-06-09
### Added
- Database-backed authentication (Iteration 2).
- `User` entity implementing `UserDetails`.
- `CustomUserDetailsService` for loading users from PostgreSQL.
- Liquibase migrations for `users` and `user_roles` tables.
- Enhanced `AuthenticationTest` with blocked and expired account scenarios.

## [0.3.0] - 2026-06-09
### Added
- Password hashing with BCrypt (Iteration 3).
- Configured `BCryptPasswordEncoder` bean.
- Updated database migrations and tests to use hashed passwords.

## [0.4.0] - 2026-06-10
### Added
- User registration (Iteration 4).
- `RegistrationController` with `POST /api/public/register`.
- `UserService` for handling registration logic and password encoding.
- `RegisterRequest` DTO with validation.
- Integration tests for registration.

## [0.5.0] - 2026-06-10
### Added
- Role-based authorization (Iteration 6).
- `AdminUserController` with `GET /api/admin/users`.
- `@EnableMethodSecurity` and `@PreAuthorize` usage.
- Authorization tests for `USER` and `ADMIN` roles.

## [0.6.0] - 2026-06-10
### Added
- Current User endpoint (Iteration 5).
- `UserController` with `GET /api/users/me` and `GET /api/users/count`.
- Usage of `@AuthenticationPrincipal` for secure user access.

## [0.7.0] - 2026-06-10
### Added
- Bank accounts and Ownership Authorization (Iteration 7).
- `BankAccount` entity and `BankAccountRepository`.
- Secure account access: users can only see their own accounts (IDOR protection).
- `AccountController` for managing bank accounts.

## [0.8.0] - 2026-06-10
### Added
- Transaction history (Iteration 8).
- `AccountTransaction` entity and `TransactionRepository`.
- Paginated transaction history with ownership verification.
- Integration tests for secure transaction access.

## [0.9.0] - 2026-06-12
### Added
- Money transfers and business logic (Iteration 9).
- `TransferService` with atomic operations and business validation.
- `TransferController` with `POST /api/transfers`.
- `TransferRequest` DTO.
- Optimistic locking with `@Version` in `BankAccount`.
- Expanded `TransactionType` with `TRANSFER_IN` and `TRANSFER_OUT`.
- `GlobalExceptionHandler` update for bad request handling.
- Integration tests for successful and failed transfers.

## [0.10.0] - 2026-06-14
### Added
- Custom security error handling (Iteration 10).
- `CustomAuthenticationEntryPoint` for 401 Unauthorized JSON responses.
- `CustomAccessDeniedHandler` for 403 Forbidden JSON responses.
- Integrated handlers into `SecurityConfig`.
- `SecurityErrorHandlingVerificationTest` for verifying JSON error structure.

## [1.0.0] - 2026-06-18
### Added
- JWT Infrastructure and Authentication Filter (Iteration 12).
- Refresh Token logic (Iteration 13).
- Hardening (Security Headers, CORS) (Iteration 14).
