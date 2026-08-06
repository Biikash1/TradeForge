###  ** 🚀 TradeForge**

   **TradeForge** — A full-stack crypto trading platform built with Java, spring boot and react, 
       enabling users to buy, sell, and track cryptocurrencies in real times.

#### Database Design & Tables

#### **📋Tables and Relationships**

1. **👤 Users Table**
    - `id` (Primary Key)
    - `fullName`
    - `email`
    - `mobile`
    - `password`
    - `status`
    - `isVerified`
    - `twoFactorAuth_enabled`
    - `twoFactorAuth_sendTo`
    - `picture`
    - `role`

2. **🪙 Coins Table**
    - `id` (Primary Key)
    - `symbol`
    - `name`
    - `image`
    - `current_price`
    - `market_cap`
    - `market_cap_rank`
    - `fully_diluted_valuation`
    - `total_volume`
    - `high_24h`
    - `low_24h`
    - `price_change_24h`
    - `price_change_percentage_24h`
    - `market_cap_change_24h`
    - `market_cap_change_percentage_24h`
    - `circulating_supply`
    - `total_supply`
    - `max_supply`
    - `ath`
    - `ath_change_percentage`
    - `ath_date`
    - `atl`
    - `atl_change_percentage`
    - `atl_date`
    - `roi`
    - `last_updated`

3. **📦 Assets Table**
    - `id` (Primary Key)
    - `quantity`
    - `buy_price`
    - `coin_id` (Foreign Key → Coins)
    - `user_id` (Foreign Key → Users)

4. **💸 Withdrawals Table**
    - `id` (Primary Key)
    - `status`
    - `amount`
    - `user_id` (Foreign Key → Users)
    - `date`

5. **⭐ Watchlist Table**
    - `id` (Primary Key)
    - `user_id` (Foreign Key → Users)

6. **🔗 Watchlist_Coins Table**
    - `watchlist_id` (Foreign Key → Watchlists)
    - `coin_id` (Foreign Key → Coins)

7. **💳 WalletTransactions Table**
    - `id` (Primary Key)
    - `wallet_id` (Foreign Key → Wallets)
    - `type`
    - `date`
    - `transfer_id`
    - `purpose`
    - `amount`

8. **👛 Wallets Table**
    - `id` (Primary Key)
    - `user_id` (Foreign Key → Users)
    - `balance`

9. **🔐 VerificationCodes Table**
    - `id` (Primary Key)
    - `otp`
    - `user_id` (Foreign Key → Users)
    - `email`
    - `mobile`
    - `verification_type`

10. **📈 TradingHistories Table**
    - `id` (Primary Key)
    - `selling_price`
    - `buying_price`
    - `coin_id` (Foreign Key → Coins)
    - `user_id` (Foreign Key → Users)

11. **💰 PaymentOrders Table**
    - `id` (Primary Key)
    - `amount`
    - `status`
    - `payment_method`
    - `user_id` (Foreign Key → Users)

12. **🏦 PaymentDetails Table**
    - `id` (Primary Key)
    - `account_number`
    - `account_holder_name`
    - `ifsc`
    - `bank_name`
    - `user_id` (Foreign Key → Users)

13. **🛒 Orders Table**
    - `id` (Primary Key)
    - `user_id` (Foreign Key → Users)
    - `order_type`
    - `price`
    - `timestamp`
    - `status`
    - `order_item_id` (Foreign Key → OrderItems)

14. **📦 OrderItems Table**
    - `id` (Primary Key)
    - `quantity`
    - `coin_id` (Foreign Key → Coins)
    - `buy_price`
    - `sell_price`
    - `order_id` (Foreign Key → Orders)

15. **🔔 Notifications Table**
    - `id` (Primary Key)
    - `from_user_id` (Foreign Key → Users)
    - `to_user_id` (Foreign Key → Users)
    - `amount`
    - `message`

16. **📊 MarketChartData Table**
    - `id` (Primary Key)
    - `timestamp`
    - `price`

17. **🔑 ForgotPasswordTokens Table**
    - `id` (Primary Key)
    - `user_id` (Foreign Key → Users)
    - `otp`
    - `verification_type`
    - `send_to`

## 📊 ER Diagram

```mermaid
erDiagram

    USERS {
        bigint id PK
        string fullName
        string email
        string mobile
        string password
        string status
        boolean isVerified
        boolean twoFactorAuth_enabled
        string twoFactorAuth_sendTo
        string picture
        string role
    }

    COINS {
        bigint id PK
        string symbol
        string name
        string image
        decimal current_price
        decimal market_cap
        int market_cap_rank
        decimal total_volume
        decimal high_24h
        decimal low_24h
        decimal price_change_24h
        decimal circulating_supply
        decimal total_supply
        decimal max_supply
        datetime last_updated
    }

    ASSETS {
        bigint id PK
        decimal quantity
        decimal buy_price
        bigint user_id FK
        bigint coin_id FK
    }

    WALLETS {
        bigint id PK
        decimal balance
        bigint user_id FK
    }

    WALLET_TRANSACTIONS {
        bigint id PK
        string type
        decimal amount
        string purpose
        datetime date
        string transfer_id
        bigint wallet_id FK
    }

    WITHDRAWALS {
        bigint id PK
        decimal amount
        string status
        datetime date
        bigint user_id FK
    }

    WATCHLISTS {
        bigint id PK
        bigint user_id FK
    }

    WATCHLIST_COINS {
        bigint watchlist_id FK
        bigint coin_id FK
    }

    VERIFICATION_CODES {
        bigint id PK
        string otp
        string email
        string mobile
        string verification_type
        bigint user_id FK
    }

    TRADING_HISTORIES {
        bigint id PK
        decimal buying_price
        decimal selling_price
        bigint coin_id FK
        bigint user_id FK
    }

    PAYMENT_ORDERS {
        bigint id PK
        decimal amount
        string status
        string payment_method
        bigint user_id FK
    }

    PAYMENT_DETAILS {
        bigint id PK
        string account_number
        string account_holder_name
        string ifsc
        string bank_name
        bigint user_id FK
    }

    ORDERS {
        bigint id PK
        string order_type
        decimal price
        string status
        datetime timestamp
        bigint user_id FK
    }

    ORDER_ITEMS {
        bigint id PK
        decimal quantity
        decimal buy_price
        decimal sell_price
        bigint order_id FK
        bigint coin_id FK
    }

    NOTIFICATIONS {
        bigint id PK
        decimal amount
        string message
        bigint from_user_id FK
        bigint to_user_id FK
    }

    MARKET_CHART_DATA {
        bigint id PK
        datetime timestamp
        decimal price
    }

    FORGOT_PASSWORD_TOKENS {
        bigint id PK
        string otp
        string verification_type
        string send_to
        bigint user_id FK
    }

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
