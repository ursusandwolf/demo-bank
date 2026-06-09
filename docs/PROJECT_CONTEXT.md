# Project Context: Demo Bank

## Current State
- **Iteration 1** completed: Basic endpoint protection implemented.
- Tech Stack: Java 21, Spring Boot 3.3.0, PostgreSQL, Liquibase, Maven, Docker Compose, Spring Security.
- Implemented a public health endpoint and a protected hello endpoint.
- Configured basic authentication with an in-memory user.

## Pending Items
- **Iteration 2**: User from Database (UserDetails, UserDetailsService).
- Database schema for Users and Roles.
- Registration and Authentication logic.

## Recent Changes
- Created `pom.xml` with core dependencies.
- Set up package-by-feature directory structure.
- Configured PostgreSQL (main) and H2 (test) data sources.
- Added `HealthController` and `GlobalExceptionHandler`.
