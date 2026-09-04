package com.cryptotrading.repository;

import com.cryptotrading.model.Wallet;
import com.cryptotrading.model.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction> findByWalletOrderByDateDesc(Wallet wallet);

    List<WalletTransaction> findByWalletIdOrderByDateDesc(Long walletId);
}