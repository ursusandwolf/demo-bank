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
    └── UserRepository
```
