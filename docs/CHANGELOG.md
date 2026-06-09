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

