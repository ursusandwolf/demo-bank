# UML Diagrams

## Package Structure
```text
com.example.bank
├── BankApplication
├── common
│   ├── exception
│   │   ├── ErrorResponse (record)
│   │   └── GlobalExceptionHandler
│   └── health
│       └── HealthController
├── auth
│   └── HelloController
├── security
│   ├── SecurityConfig
│   └── CustomUserDetailsService
└── user
    ├── User (Entity, UserDetails)
    ├── Role (Enum)
    ├── UserStatus (Enum)
    ├── UserRepository
    ├── UserService
    ├── RegistrationController
    ├── AdminUserController
    └── dto
        ├── RegisterRequest (record)
        └── UserResponse (record)
├── account
│   ├── BankAccount (Entity)
│   ├── AccountTransaction (Entity)
│   ├── AccountStatus (Enum)
│   ├── TransactionType (Enum)
│   ├── BankAccountRepository
│   ├── TransactionRepository
│   ├── AccountController
│   └── dto
│       ├── AccountResponse
│       └── TransactionResponse
```
