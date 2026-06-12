# Project Documentation: Demo Bank

## Overview
Demo Bank is a learning project designed to demonstrate the implementation of a secure banking backend using Spring Security and modern Java practices.

## Security Architecture
The project follows a "Security in Depth" approach:
1.  **Authentication**: HTTP Basic authentication (moving towards JWT in later iterations).
2.  **Authorization**: 
    - **Request-level**: Endpoint protection based on roles (`USER`, `ADMIN`).
    - **Resource-level (IDOR Protection)**: Verification that the authenticated user owns the resource they are trying to access (Accounts, Transactions).
3.  **Data Protection**:
    - Passwords are hashed using **BCrypt**.
    - Sensitive data exposure is minimized through DTOs.
4.  **Concurrency**: **Optimistic Locking** via JPA `@Version` to prevent race conditions during balance updates.

## Core Features

### User Management
- **Registration**: Public endpoint for creating new accounts.
- **Roles**: Distinct roles for users and admins.
- **Profile**: `/api/users/me` for retrieving current session info.

### Banking Operations
- **Account Management**: View owned accounts and details.
- **Transaction History**: Secure, paginated access to account history.
- **Transfers**: Atomic money transfers between accounts with full validation.

## API Endpoints

### Public
- `GET /api/public/health`: System health check.
- `POST /api/auth/register`: User registration.

### Protected (USER)
- `GET /api/users/me`: Current user info.
- `GET /api/accounts`: List owned accounts.
- `GET /api/accounts/{number}`: Account details.
- `GET /api/accounts/{number}/transactions`: Account history.
- `POST /api/transfers`: Initiate money transfer.

### Protected (ADMIN)
- `GET /api/admin/users`: List all users.

## Error Handling
The project uses a `GlobalExceptionHandler` to return consistent JSON error responses with trace IDs for debugging.
- `401 Unauthorized`: Missing or invalid credentials.
- `403 Forbidden`: Insufficient permissions (role mismatch).
- `404 Not Found`: Resource doesn't exist or belongs to another user.
- `400 Bad Request`: Business validation failure (e.g., insufficient funds).
