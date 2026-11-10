package com.cryptotrading.service;

import com.cryptotrading.model.Wallet;
import com.cryptotrading.model.WalletTransaction;
import org.springframework.data.domain.Page;

import java.util.List;

public interface WalletTransactionService {
    Page<WalletTransaction> getTransactionsByWallet(Wallet wallet, int page, int size);
}