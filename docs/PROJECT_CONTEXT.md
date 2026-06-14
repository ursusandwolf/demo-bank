# Project Context: Demo Bank

## Current State
- **Iteration 10** completed: Custom security error handling (EntryPoint and AccessDeniedHandler).
- Tech Stack: Java 21, Spring Boot 3.3.0, PostgreSQL, Liquibase, Maven, Docker Compose, Spring Security (BCrypt).
- Implemented core features: Registration, Roles, Current User, Bank Accounts, Transaction History, Transfers.
- Security: Ownership-based authorization (IDOR protection), custom JSON error responses for 401/403.
- Concurrency: Optimistic locking with `@Version` for balance consistency.

## Pending Items
- **Iteration 11**: Comprehensive Security Testing.
- **Iteration 12**: JWT transition.

## Recent Changes
- Implemented `CustomAuthenticationEntryPoint` and `CustomAccessDeniedHandler`.
- Configured `SecurityConfig` to return consistent JSON errors for authentication and authorization failures.
- Added `SecurityErrorHandlingVerificationTest`.
- Fixed `CHANGELOG.md` versioning and updated documentation.
