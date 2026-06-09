# Project Context: Demo Bank

## Current State
- **Iteration 0** completed: Project skeleton initialized.
- Tech Stack: Java 21, Spring Boot 3.3.0, PostgreSQL, Liquibase, Maven, Docker Compose.
- Implemented a public health endpoint and global exception handling.
- Added `docker-compose.yml` for local database setup.
- Basic test suite is in place and passing.

## Pending Items
- **Iteration 1**: First protection of endpoints (Spring Security basics).
- Database schema for Users and Roles.
- Registration and Authentication logic.

## Recent Changes
- Created `pom.xml` with core dependencies.
- Set up package-by-feature directory structure.
- Configured PostgreSQL (main) and H2 (test) data sources.
- Added `HealthController` and `GlobalExceptionHandler`.
