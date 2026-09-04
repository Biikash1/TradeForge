package com.cryptotrading.service;

import com.cryptotrading.model.Wallet;
import com.cryptotrading.model.WalletTransaction;

import java.util.List;

public interface WalletTransactionService {
    List<WalletTransaction> getTransactionsByWallet(Wallet wallet);
}