<div align="center">

# TradeForge ⚡

**A full-stack cryptocurrency trading and portfolio management platform.**

Execute trades, manage digital wallets, track real-time market data, and process secure fiat-to-crypto payments.

[![Backend: Spring Boot](https://img.shields.io/badge/Backend-Spring%20Boot-6DB33F?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Frontend: React](https://img.shields.io/badge/Frontend-React-61DAFB?style=flat-square&logo=react)](https://reactjs.org/)
[![Database: MySQL / PostgreSQL](https://img.shields.io/badge/Database-MySQL%20%7C%20PostgreSQL-336791?style=flat-square&logo=postgresql)](#-database-architecture)
[![Security: JWT & 2FA](https://img.shields.io/badge/Security-JWT%20%26%202FA-black?style=flat-square&logo=jsonwebtokens)](#-security--authentication)
[![Java 21](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk)](https://openjdk.org/)

</div>

---

## 📑 Table of Contents

- [Overview](#-overview)
- [Core Features](#-core-features)
- [Technology Stack](#-technology-stack)
- [Security & Authentication](#-security--authentication)
- [Payment Processing Engine](#-payment-processing-engine)
- [Database Architecture](#-database-architecture)
- [Getting Started](#-getting-started)
- [Configuration](#-configuration)
- [Project Structure](#-project-structure)
- [Roadmap](#-roadmap)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🔭 Overview

TradeForge is engineered for scale and correctness. It combines a stateless, secure authentication layer, a transactional order execution engine, a double-entry wallet ledger, and an idempotent multi-gateway payment pipeline behind a React-based trading terminal.

---

## 🚀 Core Features

| Feature | Description |
| :--- | :--- |
| **Real-Time Market Tracking** | Live cryptocurrency pricing, market cap rankings, and historical chart data. |
| **Order Execution Engine** | Buy and sell orders with full order history and transactional integrity. |
| **Double-Entry Wallet System** | Internal ledger for user balances, top-ups, and withdrawal processing. |
| **Institutional-Grade Security** | Stateless JWT authentication, BCrypt password hashing, and OTP-based Two-Factor Authentication (2FA). |
| **Multi-Gateway Payments** | Idempotent fiat deposits via **Razorpay** and **Stripe** with automated wallet crediting. |
| **Watchlists & Portfolio** | Per-user watchlists and asset holdings with buy-price tracking. |

---

## 🛠️ Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend Core** | Java 21, Spring Boot, Spring Data JPA, Hibernate |
| **Frontend Terminal** | React, Redux (state management), Tailwind CSS |
| **Security** | Spring Security, JWT, BCrypt, OTP via Email/SMS |
| **Database** | Relational database (MySQL / PostgreSQL) |
| **Payment Gateways** | Razorpay API, Stripe API |

---

## 🔐 Security & Authentication

TradeForge uses a stateless authentication mechanism. Sign-in supports optional Two-Factor Authentication (2FA): when enabled, an OTP must be verified before the final JWT is issued.

```mermaid
flowchart TD
    C(["Client<br/>Postman / React"])

    C -->|"POST /auth/signup"| S1["AuthController"]
    C -->|"POST /auth/signin"| L1["AuthController"]

    subgraph SIGNUP ["Sign-up flow"]
        S1 --> S2["Validate RegisterRequest"]
        S2 --> S3{"Email already exists?"}
        S3 -->|Yes| SE["Error response"]
        S3 -->|No| S4["BCrypt-encode password"]
        S4 --> S5["Save user to database"]
        S5 --> S6["Create Authentication"]
    end

    subgraph SIGNIN ["Sign-in flow"]
        L1 --> L2["Validate AuthRequest"]
        L2 --> L3["Authenticate user<br/>(verify BCrypt password)"]
        L3 --> L4["Find user"]
        L4 --> L5{"2FA enabled?"}
        L5 -->|Yes| L6["Generate OTP"]
        L6 --> L7["Send OTP via email"]
        L7 --> L8["Return session"]
        L8 --> L9["Client submits OTP"]
        L9 --> L10{"OTP valid?"}
        L10 -->|No| LE["Error response"]
    end

    S6 --> J["JwtProvider"]
    L5 -->|No| J
    L10 -->|Yes| J

    J --> J1["Sign with secret key"]
    J1 --> J2["Return JWT"]
    J2 --> C2(["Client stores JWT"])
```

---

## 💳 Payment Processing Engine

The platform implements a resilient, **idempotent** payment engine. Internal wallets are credited only after successful verification with the payment provider, which prevents race conditions and duplicate credits.

```mermaid
flowchart TD
    A["Payment request"] --> B["Create PaymentOrder<br/>status: PENDING<br/>walletCredited: false"]
    B --> C{"Payment method"}

    C -->|RAZORPAY| D["Razorpay payment link"]
    C -->|STRIPE| E["Stripe checkout session"]

    D --> G["PaymentService<br/>processPaymentAndCreditWallet()"]
    E --> G

    G --> I{"Order exists?"}
    I -->|No| J["PaymentOrderNotFoundException"]
    I -->|Yes| L{"User owns order?"}
    L -->|No| M["PaymentOwnershipException"]
    L -->|Yes| O{"Wallet already credited?"}

    O -->|Yes| P["Return existing wallet<br/>(no duplicate credit)"]
    O -->|No| R{"Payment method"}

    R -->|RAZORPAY| S["Verify amount → currency → payment status"]
    R -->|STRIPE| T["Verify amount → currency → order ID → payment status"]

    S --> U{"Payment valid?"}
    T --> U

    U -->|No| V["Mark FAILED<br/>PaymentVerificationException"]
    U -->|Yes| W["Payment SUCCESS"]

    W --> X["walletService.addBalance()"]
    X --> Y["Set walletCredited = true"]
    Y --> Z["Save PaymentOrder"]
    Z --> AA["Return WalletResponse"]

    J --> AB["Global Exception Handler"]
    M --> AB
    V --> AB
    AB --> AC["Standard API error response"]

    classDef start fill:#1f2937,stroke:#60a5fa,stroke-width:2px,color:#fff;
    classDef process fill:#111827,stroke:#9ca3af,stroke-width:1.5px,color:#fff;
    classDef decision fill:#1f2937,stroke:#fbbf24,stroke-width:2px,color:#fff;
    classDef success fill:#064e3b,stroke:#34d399,stroke-width:2px,color:#fff;
    classDef error fill:#7f1d1d,stroke:#f87171,stroke-width:2px,color:#fff;

    class A start;
    class B,D,E,G,P,S,T,X,Y,Z,AA process;
    class C,I,L,O,R,U decision;
    class W success;
    class J,M,V,AB,AC error;
```

**Key guarantees**

- **Ownership check:** a user can only settle their own payment orders.
- **Idempotency:** the `walletCredited` flag guarantees a wallet is credited at most once per order.
- **Provider-side verification:** amount, currency, and payment status (plus order ID for Stripe) are verified before any balance change.
- **Centralized errors:** all failures flow through a global exception handler and return a standard API error response.

---

## 🗄️ Database Architecture

A fully normalized relational schema (3NF) of **17 tables** across five domains:

| Domain | Tables |
| :--- | :--- |
| **Identity & Security** | `USERS`, `VERIFICATION_CODES`, `FORGOT_PASSWORD_TOKENS` |
| **Market Data** | `COINS`, `WATCHLISTS`, `WATCHLIST_COINS`, `MARKET_CHART_DATA` |
| **Execution & Trading** | `ORDERS`, `ORDER_ITEMS`, `TRADING_HISTORIES`, `ASSETS` |
| **Finance & Ledger** | `WALLETS`, `WALLET_TRANSACTIONS`, `WITHDRAWALS`, `PAYMENT_ORDERS`, `PAYMENT_DETAILS` |
| **Communication** | `NOTIFICATIONS` |

### Key Entities

| Entity | Key Columns |
| :--- | :--- |
| **Users** | `id`, `fullName`, `email`, `mobile`, `password`, `status`, `isVerified`, `twoFactorAuth_enabled`, `twoFactorAuth_sendTo`, `picture`, `role` |
| **Coins** | `id`, `symbol`, `name`, `current_price`, `market_cap`, `total_volume`, `circulating_supply`, `ath`, `atl`, … (plus historical metrics) |
| **Assets (Holdings)** | `id`, `quantity`, `buy_price`, `coin_id`, `user_id` |
| **Wallets** | `id`, `user_id`, `balance` |
| **Wallet Transactions** | `id`, `wallet_id`, `type`, `amount`, `purpose`, `date` |
| **Orders** | `id`, `user_id`, `order_type`, `price`, `status`, `timestamp` |
| **Payments & Withdrawals** | Fiat deposits in `PAYMENT_ORDERS`; bank-linked account details in `PAYMENT_DETAILS` and `WITHDRAWALS` |

### Entity-Relationship Diagram

```mermaid
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
```

---

## 🏁 Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+ (or the included wrapper)
- Node.js 18+ and npm
- MySQL 8+ **or** PostgreSQL 14+
- Razorpay and/or Stripe test-mode accounts

### 1. Clone the repository

```bash
git clone https://github.com/<your-username>/tradeforge.git
cd tradeforge
```

### 2. Set up the database

```sql
CREATE DATABASE tradeforge;
```

### 3. Run the backend

```bash
cd backend
./mvnw clean install
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080` by default.

### 4. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

The app starts on `http://localhost:5173` (Vite) or `http://localhost:3000` (CRA), depending on your setup.

---

## ⚙️ Configuration

Never commit secrets. Provide them through environment variables or an untracked `application-local.properties`.

| Variable | Description |
| :--- | :--- |
| `DB_URL` | JDBC URL, e.g. `jdbc:postgresql://localhost:5432/tradeforge` |
| `DB_USERNAME` / `DB_PASSWORD` | Database credentials |
| `JWT_SECRET` | Secret key used to sign JWTs (use a long, random value) |
| `JWT_EXPIRATION_MS` | Token lifetime in milliseconds |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP credentials used to send OTP emails |
| `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` | Razorpay API credentials |
| `STRIPE_SECRET_KEY` | Stripe secret API key |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook signing secret |
| `COIN_API_KEY` | Market data provider key (if your provider requires one) |

> **Note:** adjust variable names to match your `application.properties`.

---

## 📁 Project Structure

```text
tradeforge/
├── backend/                  # Spring Boot application
│   └── src/main/java/…
│       ├── controller/       # REST controllers
│       ├── service/          # Business logic (payments, wallet, orders)
│       ├── repository/       # Spring Data JPA repositories
│       ├── model/            # JPA entities
│       ├── request/          # Request DTOs
│       ├── response/         # Response DTOs
│       ├── config/           # Security & JWT configuration
│       └── exception/        # Custom exceptions & global handler
├── frontend/                 # React + Redux + Tailwind terminal
└── README.md
```

> Update this tree to match your actual repository layout.

---

## 🗺️ Roadmap

- [ ] Limit and stop-loss order types
- [ ] WebSocket-based live price streaming
- [ ] Portfolio analytics and P&L dashboards
- [ ] Dockerized deployment (Docker Compose)
- [ ] CI/CD pipeline and automated test coverage reports

---

## 🤝 Contributing

Contributions are welcome.

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m "feat: add your feature"`
4. Push the branch: `git push origin feature/your-feature`
5. Open a Pull Request

---

## 📄 License

Distributed under the MIT License. See [`LICENSE`](LICENSE) for details.

---

<div align="center">

Built with ☕ Java and ⚛️ React

</div>
