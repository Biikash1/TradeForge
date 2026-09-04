package com.cryptotrading.service;

import com.cryptotrading.model.Wallet;
import com.cryptotrading.model.WalletTransaction;
import com.cryptotrading.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletTransactionServiceImpl implements WalletTransactionService {

    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    public List<WalletTransaction> getTransactionsByWallet(Wallet wallet) {
        if (wallet == null) {
            return List.of();
        }
        return walletTransactionRepository.findByWalletOrderByDateDesc(wallet);
    }
}