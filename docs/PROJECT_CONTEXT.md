# Project Context: Demo Bank

## Current State
- **Iteration 8** completed: Transaction history with pagination and ownership authorization implemented.
- Tech Stack: Java 21, Spring Boot 3.3.0, PostgreSQL, Liquibase, Maven, Docker Compose, Spring Security (BCrypt).
- Implemented core features: Registration, Roles, Current User, Bank Accounts, Transaction History.
- Security: Ownership-based authorization (IDOR protection) for accounts and transactions.

## Pending Items
- **Iteration 9**: Transfers and business logic.
- **Iteration 10**: Security error handling.
- **Iteration 12**: JWT transition.

## Recent Changes
- Added `BankAccount` and `AccountTransaction` entities.
- Implemented `AccountController` with secure access to accounts and paginated transactions.
- Added comprehensive authorization tests for ownership verification.
- Updated documentation and project UML.
