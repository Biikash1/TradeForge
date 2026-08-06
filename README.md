###  **TradeForge**

   **TradeForge** — A full-stack crypto trading platform built with Java, spring boot and react, 
       enabling users to buy, sell, and track cryptocurrencies in real times.

#### Database Design & Tables

#### **Tables and Relationships**

1. **Users Table**
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
2. **Coins Table**
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
3. **Assets Table**
    - `id` (Primary Key)
    - `quantity`
    - `buy_price`
    - `coin_id` (Foreign Key referencing Coins)
    - `user_id` (Foreign Key referencing Users)
4. **Withdrawals Table**
    - `id` (Primary Key)
    - `status`
    - `amount`
    - `user_id` (Foreign Key referencing Users)
    - `date`
5. **Watchlists Table**
    - `id` (Primary Key)
    - `user_id` (Foreign Key referencing Users)
6. **Watchlist_Coins Table** (Join Table for many-to-many relationship)
    - `watchlist_id` (Foreign Key referencing Watchlists)
    - `coin_id` (Foreign Key referencing Coins)
7. **WalletTransactions Table**
    - `id` (Primary Key)
    - `wallet_id` (Foreign Key referencing Wallets)
    - `type`
    - `date`
    - `transfer_id`
    - `purpose`
    - `amount`
8. **Wallets Table**
    - `id` (Primary Key)
    - `user_id` (Foreign Key referencing Users)
    - `balance`
9. **VerificationCodes Table**
    - `id` (Primary Key)
    - `otp`
    - `user_id` (Foreign Key referencing Users)
    - `email`
    - `mobile`
    - `verification_type`
10. **TradingHistories Table**
    - `id` (Primary Key)
    - `selling_price`
    - `buying_price`
    - `coin_id` (Foreign Key referencing Coins)
    - `user_id` (Foreign Key referencing Users)
11. **PaymentOrders Table**
    - `id` (Primary Key)
    - `amount`
    - `status`
    - `payment_method`
    - `user_id` (Foreign Key referencing Users)
12. **PaymentDetails Table**
    - `id` (Primary Key)
    - `account_number`
    - `account_holder_name`
    - `ifsc`
    - `bank_name`
    - `user_id` (Foreign Key referencing Users)
13. **Orders Table**
    - `id` (Primary Key)
    - `user_id` (Foreign Key referencing Users)
    - `order_type`
    - `price`
    - `timestamp`
    - `status`
    - `order_item_id` (Foreign Key referencing OrderItems)
14. **OrderItems Table**
    - `id` (Primary Key)
    - `quantity`
    - `coin_id` (Foreign Key referencing Coins)
    - `buy_price`
    - `sell_price`
    - `order_id` (Foreign Key referencing Orders)
15. **Notifications Table**
    - `id` (Primary Key)
    - `from_user_id` (Foreign Key referencing Users)
    - `to_user_id` (Foreign Key referencing Users)
    - `amount`
    - `message`
16. **MarketChartData Table**
    - `id` (Primary Key)
    - `timestamp`
    - `price`
17. **ForgotPasswordTokens Table**
    - `id` (Primary Key)
    - `user_id` (Foreign Key referencing Users)
    - `otp`
    - `verification_type`
    - `send_to`
  
#####   **ER-Diagram**

+---------------------+           +-----------------+
|       Users         |<--------->|    Wallets      |
|---------------------|           +-----------------+
| id                  |               ^            
| fullName            |               |
| email               |               |         
| ...                 |               |
+---------------------+               |
|
+--------------------+            +--------------------+
|      Assets        |<---------->| WalletTransactions |
|--------------------|            +--------------------+
| id                 |
| quantity           |
| buy_price          |<---------->+-----------------+
| coin_id            |            |  Coins          |
| user_id            |            +-----------------+
+--------------------+            | id              |
                                  | symbol          |
+--------------------+            | ...             |
| Withdrawals        |<---------->+-----------------+
|--------------------|
| id                 |
| status             |
| amount             |
| user_id            |
| date               |
+--------------------+

+--------------------+
| Watchlist         |
|--------------------+
| id                 |
| user_id            |
+--------------------+
       |
       |
       v
+--------------------+
| Watchlist_Coins    |
|--------------------+
| watchlist_id       |
| coin_id            |
+--------------------+

+---------------------+           +---------------------+
|   VerificationCodes |<--------->|        Users        |
|---------------------|           +---------------------+
| id                  |
| otp                 |
| user_id             |
| email               |
| mobile              |
| verification_type   |
+---------------------+

+---------------------+           +---------------------+
|  TradingHistories   |<--------->|        Users        |
|---------------------|           +---------------------+
| id                  |
| selling_price       |
| buying_price        |
| coin_id             |
| user_id             |
+---------------------+

+---------------------+           +---------------------+
|    PaymentOrders    |<--------->|        Users        |
|---------------------|           +---------------------+
| id                  |
| amount              |
| status              |
| payment_method      |
| user_id             |
+---------------------+

+---------------------+           +---------------------+
|   PaymentDetails    |<--------->|        Users        |
|---------------------|           +---------------------+
| id                  |
| account_number      |
| account_holder_name |
| ifsc                |
| bank_name           |
| user_id             |
+---------------------+

+---------------------+           +---------------------+
|        Orders       |<--------->|        Users        |
|---------------------|           +---------------------+
| id                  |
| user_id             |
| order_type          |
| price               |
| timestamp           |
| status              |
| order_item_id       |
+---------------------+
        |
        |
        v
+---------------------+           +---------------------+
|      OrderItems     |<--------->|        Coins        |
|---------------------|           +---------------------+
| id                  |
| quantity            |
| coin_id             |
| buy_price           |
| sell_price          |
| order_id            |
+---------------------+

+---------------------+             +---------------------+
|    Notifications    | <---------> |        Users        |
|---------------------|             +---------------------+
| id                  |
| from_user_id        |
| to_user_id          |
| amount              |
| message             |
+---------------------+

+---------------------+           
|   MarketChartData   |
|---------------------|
| id                  |
| timestamp           |
| price               |
+---------------------+

+---------------------+             +---------------------+
| ForgotPasswordTokens| <---------> |        Users        |
|---------------------|             +---------------------+
| id                  |
| user_id             |
| otp                 |
| verification_type   |
| send_to             |
+---------------------+
