package com.cryptotrading.service;

import com.cryptotrading.model.Wallet;
import com.cryptotrading.model.WalletTransaction;
import com.cryptotrading.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletTransactionServiceImpl implements WalletTransactionService {

    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    public List<WalletTransaction> getTransactionsByWallet(Wallet wallet) {
        if (wallet == null) {
            log.warn("getTransactionsByWallet failed: Provided wallet is null.");
            return List.of();
        }

        log.debug("Fetching wallet transactions for Wallet ID: {}", wallet.getId());
        List<WalletTransaction> transactions = walletTransactionRepository.findByWalletOrderByDateDesc(wallet);

        log.info("Successfully fetched {} transactions for Wallet ID: {}", transactions.size(), wallet.getId());
        return transactions;
    }
}