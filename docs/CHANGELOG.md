# Changelog

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
