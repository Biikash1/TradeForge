# TradeForge ⚡

[![Tech Stack: Java & Spring Boot](https://img.shields.io/badge/Backend-Spring%20Boot-6DB33F?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Tech Stack: React](https://img.shields.io/badge/Frontend-React-61DAFB?style=flat-square&logo=react)](https://reactjs.org/)
[![Database: MySQL/PostgreSQL](https://img.shields.io/badge/Database-Relational-336791?style=flat-square&logo=database)](#)
[![Security: JWT](https://img.shields.io/badge/Security-JWT%20%26%202FA-black?style=flat-square&logo=jsonwebtokens)](#)

> **TradeForge** is a full-stack algorithmic cryptocurrency trading and portfolio management platform. Engineered for scale, it enables users to execute trades, manage digital wallets, track real-time market data, and process secure fiat-to-crypto payments.

---

## 🚀 Core Features

- **Real-Time Market Tracking:** Live cryptocurrency pricing, market cap rankings, and historical chart data integration.
- **Order Execution Engine:** Support for buying, selling, and tracking complex order histories with transactional integrity.
- **Double-Entry Wallet System:** Secure internal ledger for managing user balances, top-ups, and withdrawal processing.
- **Institutional-Grade Security:** Stateless JWT authentication, BCrypt password hashing, and OTP-based Two-Factor Authentication (2FA).
- **Multi-Gateway Payment Processing:** Idempotent fiat deposit processing via **Razorpay** and **Stripe** with automated wallet crediting.

---

## 🛠️ Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend Core** | Java 17+, Spring Boot, Spring Data JPA, Hibernate |
| **Frontend Terminal**| React, Redux (State Management), Tailwind CSS |
| **Security** | Spring Security, JWT, BCrypt, OTP via Email/SMS |
| **Database** | Relational Database (MySQL / PostgreSQL) |
| **Payment Gateways** | Razorpay API, Stripe API |

---

## 🔐 Security & Authentication Architecture

TradeForge utilizes a highly secure, stateless authentication mechanism. The flow supports optional Two-Factor Authentication (2FA) before issuing the final JWT session token.

```text
                         ┌──────────────────────┐
                         │        CLIENT        │
                         │    Postman / React   │
                         └──────────┬───────────┘
                                    │
                  ┌─────────────────┴─────────────────┐
                  │                                   │
             POST /auth/signup                  POST /auth/signin
                  │                                   │
                  ▼                                   ▼
        ┌───────────────────┐               ┌───────────────────┐
        │  AuthController   │               │  AuthController   │
        └─────────┬─────────┘               └─────────┬─────────┘
                  │                                   │
                  ▼                                   ▼
        Validate RegisterRequest             Validate AuthRequest
                  │                                   │
                  ▼                                   ▼
        Check email exists                    Authenticate user
                  │                                   │
                  ▼                                   ▼
        BCrypt encode password               Verify BCrypt password
                  │                                   │
                  ▼                                   ▼
        Save User to Database                      Find User
                  │                                   │
                  ▼                                   ▼
        Create Authentication               Check 2FA enabled?
                  │                         ┌─────────┴─────────┐
                  │                         │                   │
                  │                        YES                  NO
                  │                         │                   │
                  │                         ▼                   ▼
                  │                   Generate OTP         Generate JWT
                  │                         │                   │
                  │                   Send OTP Email            │
                  │                         │                   │
                  │                   Return Session            │
                  │                         │                   │
                  │                   Verify OTP                │
                  │                         │                   │
                  │                         ▼                   ▼
                  │                   Generate JWT              │
                  │                         │                   │
                  └──────────────┬──────────┴───────────────────┘
                                 │
                                 ▼
                         ┌─────────────────┐
                         │   JwtProvider   │
                         └────────┬────────┘
                                  │
                                  ▼
                         Sign with SecretKey
                                  │
                                  ▼
                           Return JWT Token
                                  │
                                  ▼
                         ┌─────────────────┐
                         │      CLIENT     │
                         │    Store JWT    │
                         └─────────────────┘

💳 Payment Processing Engine
The platform implements a resilient, idempotent payment processing engine. It ensures that internal wallets are only credited upon successful webhook/API verification from the provider, preventing race conditions or duplicate credits.

Code snippet
flowchart TD

    A["PAYMENT REQUEST"]
    B["PaymentOrder<br/>Status: PENDING<br/>walletCredited: false"]
    C{"PAYMENT METHOD"}

    A --> B
    B --> C

    C -->|RAZORPAY| D["Razorpay Payment Link"]
    C -->|STRIPE| E["Stripe Checkout Session"]

    D --> F["Provider Verification"]
    E --> F

    F --> G["PaymentService<br/>processPaymentAndCreditWallet()"]

    G --> H["PaymentOrder Validation"]

    H --> I{"ORDER EXISTS?"}

    I -->|NO| J["PaymentOrderNotFoundException"]
    I -->|YES| K["Ownership Check"]

    K --> L{"USER OWNS ORDER?"}

    L -->|NO| M["PaymentOwnershipException"]
    L -->|YES| N["Idempotency Check"]

    N --> O{"WALLET ALREADY CREDITED?"}

    O -->|YES| P["Return Existing Wallet<br/>No Duplicate Credit"]
    O -->|NO| Q["Provider Verification"]

    Q --> R{"PAYMENT METHOD"}

    R -->|RAZORPAY| S["Razorpay Verification"]
    R -->|STRIPE| T["Stripe Verification"]

    S --> S1["Verify Amount"]
    S1 --> S2["Verify Currency"]
    S2 --> S3["Verify Payment Status"]

    T --> T1["Verify Amount"]
    T1 --> T2["Verify Currency"]
    T2 --> T3["Verify Order ID"]
    T3 --> T4["Verify Payment Status"]

    S3 --> U{"PAYMENT VALID?"}
    T4 --> U

    U -->|NO| V["Payment FAILED<br/>PaymentVerificationException"]
    U -->|YES| W["Payment SUCCESS"]

    W --> X["Credit Wallet<br/>walletService.addBalance()"]
    X --> Y["Mark Wallet Credited<br/>walletCredited = true"]
    Y --> Z["Save PaymentOrder"]
    Z --> AA["WalletResponse"]

    J --> AB["Global Exception Handler"]
    M --> AB
    V --> AB

    AB --> AC["Standard API Error Response"]

%% Styling
    classDef start fill:#1f2937,stroke:#60a5fa,stroke-width:2px,color:#fff;
    classDef process fill:#111827,stroke:#9ca3af,stroke-width:1.5px,color:#fff;
    classDef decision fill:#1f2937,stroke:#fbbf24,stroke-width:2px,color:#fff;
    classDef success fill:#064e3b,stroke:#34d399,stroke-width:2px,color:#fff;
    classDef error fill:#7f1d1d,stroke:#f87171,stroke-width:2px,color:#fff;

    class A start;
    class B,D,E,F,G,H,K,N,Q,S,T,S1,S2,S3,T1,T2,T3,T4,X,Y,Z,AA,P process;
    class C,I,L,O,R,U decision;
    class W success;
    class J,M,V,AB,AC error;
    
🗄️ Database Architecture
The system utilizes a fully normalized relational database schema (3NF) containing 17 tables, divided into four core business domains:

Identity & Security: USERS, VERIFICATION_CODES, FORGOT_PASSWORD_TOKENS

Market Data: COINS, WATCHLISTS, WATCHLIST_COINS, MARKET_CHART_DATA

Execution & Trading: ORDERS, ORDER_ITEMS, TRADING_HISTORIES, ASSETS

Finance & Ledger: WALLETS, WALLET_TRANSACTIONS, WITHDRAWALS, PAYMENT_ORDERS, PAYMENT_DETAILS

Users: id, fullName, email, mobile, password, status, isVerified, twoFactorAuth_enabled, twoFactorAuth_sendTo, picture, role

Coins: id, symbol, name, current_price, market_cap, total_volume, circulating_supply, ath, atl... (and historical metrics)

Assets (Holdings): id, quantity, buy_price, coin_id, user_id

Wallets & Transactions: id, user_id, balance | wallet_id, type, amount, purpose, date

Orders & Execution: id, user_id, order_type, price, status, timestamp

Payments & Withdrawals: Track fiat deposits (PAYMENT_ORDERS) and bank-linked account details (PAYMENT_DETAILS, WITHDRAWALS).

Entity-Relationship Diagram
Code snippet
erDiagram
    USERS ||--|| WALLETS : owns
    WALLETS ||--o{ WALLET_TRANSACTIONS : contains
    USERS ||--o{ ASSETS : owns
    COINS ||--o{ ASSETS : asset
    USERS ||--o{ WITHDRAWALS : requests
    USERS ||--|| WATCHLISTS : has
    WATCHLISTS ||--o{ WATCHLIST_COINS : contains
    COINS ||--o{ WATCHLIST_COINS : listed_in
    USERS ||--o{ VERIFICATION_CODES : receives
    USERS ||--o{ TRADING_HISTORIES : performs
    COINS ||--o{ TRADING_HISTORIES : traded
    USERS ||--o{ PAYMENT_ORDERS : creates
    USERS ||--|| PAYMENT_DETAILS : owns
    USERS ||--o{ ORDERS : places
    ORDERS ||--|{ ORDER_ITEMS : contains
    COINS ||--o{ ORDER_ITEMS : traded
    USERS ||--o{ NOTIFICATIONS : sends
    USERS ||--o{ NOTIFICATIONS : receives
    USERS ||--o{ FORGOT_PASSWORD_TOKENS : receives