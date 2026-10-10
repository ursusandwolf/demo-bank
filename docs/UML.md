# Architecture and data model

## Packages

| Package | Responsibility |
| --- | --- |
| `auth` | Registration alias, login, refresh and private example endpoint |
| `security` | Security chain, user lookup, JWT validation and refresh-token lifecycle |
| `user` | User entity, registration service, profiles and administrative queries |
| `account` | Account/history entities, owner-scoped repositories and DTOs |
| `transfer` | Atomic balance changes and paired transaction records |
| `common` | Health endpoint and application error responses |

## Persistence

```mermaid
erDiagram
    USERS ||--o{ USER_ROLES : has
    USERS ||--o{ ACCOUNTS : owns
    USERS ||--o{ REFRESH_TOKENS : authenticates
    ACCOUNTS ||--o{ TRANSACTIONS : records
    USERS {
        uuid id PK
        string email UK
        string password
        string status
        boolean account_locked
        boolean account_expired
        boolean credentials_expired
        bigint version
    }
    USER_ROLES {
        uuid user_id FK
        string role
    }
    ACCOUNTS {
        uuid id PK
        string account_number UK
        uuid owner_id FK
        decimal balance
        string currency
        string status
        bigint version
    }
    TRANSACTIONS {
        uuid id PK
        uuid account_id FK
        string type
        decimal amount
        decimal balance_after
    }
    REFRESH_TOKENS {
        uuid id PK
        uuid user_id FK
        string token UK
        timestamp expiry_date
    }
```

Passwords are BCrypt hashes. `refresh_tokens.token` holds a SHA-256 hash of a high-entropy opaque token. User and account versions support optimistic locking. Audit timestamps are populated by Spring Data JPA auditing. Liquibase owns schema changes; Hibernate validates rather than creates the schema.

## Request flow

```mermaid
sequenceDiagram
    participant C as Client
    participant F as JWT filter
    participant U as User repository
    participant T as Transfer service
    participant D as Database
    C->>F: POST /api/transfers + Bearer JWT
    F->>F: Verify signature and expiration
    F->>U: Load user and check status
    F->>T: Authenticated request
    T->>D: Read owned source and destination
    T->>T: Validate accounts, currency and amount
    T->>D: Update versioned balances and record both sides
    D-->>T: Commit or optimistic conflict
    T-->>C: 200 or 409
```

Account history queries constrain both account number and owner email. Refresh rotation uses a pessimistic row lock and replaces the stored token hash inside a transaction. The tests run migrations on H2; verification against PostgreSQL remains separate work.
