# Project Context: Demo Bank

## Current State
- **Iteration 9** completed: Money transfers with business validation and atomicity.
- Tech Stack: Java 21, Spring Boot 3.3.0, PostgreSQL, Liquibase, Maven, Docker Compose, Spring Security (BCrypt).
- Implemented core features: Registration, Roles, Current User, Bank Accounts, Transaction History, Transfers.
- Security: Ownership-based authorization (IDOR protection) for accounts, transactions, and transfers.
- Concurrency: Optimistic locking with `@Version` for balance consistency.

## Pending Items
- **Iteration 10**: Security error handling (custom EntryPoint and AccessDeniedHandler).
- **Iteration 11**: Comprehensive Security Testing.
- **Iteration 12**: JWT transition.

## Recent Changes
- Implemented `TransferService` and `TransferController`.
- Added business logic for transfers (balance check, currency check, active status).
- Added `TRANSFER_IN` and `TRANSFER_OUT` types to operation history.
- Improved `GlobalExceptionHandler` to handle business logic errors as 400 Bad Request.
- Added `TransferTest` covering positive and negative scenarios.
