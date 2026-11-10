package com.cryptotrading.service;

import com.cryptotrading.model.Wallet;
import com.cryptotrading.model.WalletTransaction;
import com.cryptotrading.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletTransactionServiceImpl implements WalletTransactionService {

    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    public Page<WalletTransaction> getTransactionsByWallet(Wallet wallet, int page, int size) {
        if (wallet == null) {
            log.warn("getTransactionsByWallet failed: Provided wallet is null.");
            return Page.empty();
        }

        log.debug("Fetching wallet transactions for Wallet ID: {}", wallet.getId());
        Pageable pageable = PageRequest.of(page, size);
        Page<WalletTransaction> transactions = walletTransactionRepository.findByWalletOrderByDateDesc(wallet, pageable);

        log.info("Successfully fetched {} transactions for Wallet ID: {}", transactions.getNumberOfElements(), page, wallet.getId());
        return transactions;
    }
}