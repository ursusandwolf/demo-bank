# Project Context: Demo Bank

## Current State
- **Iteration 3** completed: Password hashing with BCrypt implemented.
- Tech Stack: Java 21, Spring Boot 3.3.0, PostgreSQL, Liquibase, Maven, Docker Compose, Spring Security (BCrypt).
- Implemented secure password storage.
- Updated authentication tests to verify BCrypt matching.

## Pending Items
- **Iteration 4**: User Registration (Public endpoint, validation).
- Database schema for Users and Roles.
- Registration and Authentication logic.

## Recent Changes
- Created `pom.xml` with core dependencies.
- Set up package-by-feature directory structure.
- Configured PostgreSQL (main) and H2 (test) data sources.
- Added `HealthController` and `GlobalExceptionHandler`.
