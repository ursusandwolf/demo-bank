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
│   ├── CustomUserDetailsService
│   ├── CustomAuthenticationEntryPoint
│   └── CustomAccessDeniedHandler
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
│   ├── TransactionType (Enum: CREDIT, DEBIT, TRANSFER_IN, TRANSFER_OUT)
│   ├── BankAccountRepository
│   ├── TransactionRepository
│   ├── AccountController
│   └── dto
│       ├── AccountResponse
│       ├── TransactionResponse
│       └── TransferRequest
├── transfer
│   ├── TransferService
│   └── TransferController
```
