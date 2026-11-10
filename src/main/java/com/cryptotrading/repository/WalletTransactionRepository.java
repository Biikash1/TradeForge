package com.cryptotrading.repository;

import com.cryptotrading.model.Wallet;
import com.cryptotrading.model.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Page<WalletTransaction> findByWalletOrderByDateDesc(Wallet wallet, Pageable pageable);

    Page<WalletTransaction> findByWalletIdOrderByDateDesc(Long walletId, Pageable pageable);
}