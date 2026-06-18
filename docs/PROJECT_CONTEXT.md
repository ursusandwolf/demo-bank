# Project Context

## Overview
A secure "Bank User Account" REST API developed using Spring Boot 3.3.0 and Spring Security.

## Implemented Security Features
- **Authentication**: Stateless JWT authentication with Refresh Token rotation.
- **Registration**: Secure public user registration with password encoding (BCrypt) and role assignment (ROLE_USER).
- **Authorization**:
    - Role-Based Access Control (RBAC) using `@PreAuthorize` for Admin vs. User endpoints.
    - Strict Ownership Authorization (preventing IDOR) for Bank Account retrieval.
- **Hardening**:
    - Standard security headers.
    - CORS configuration (restricted to `http://localhost:3000`).
    - Consistent JSON error handling for 401 (Unauthorized) and 403 (Forbidden) via custom entry points/handlers.
- **Data Integrity**: JPA Transactions for atomic transfers, business-level validation.

## Status
- **Development**: Complete.
- **Branch**: `iteration-11`.
