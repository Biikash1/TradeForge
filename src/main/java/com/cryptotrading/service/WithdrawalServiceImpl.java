package com.cryptotrading.service;

import com.cryptotrading.domain.WithdrawalStatus;
import com.cryptotrading.model.User;
import com.cryptotrading.model.Wallet;
import com.cryptotrading.model.Withdrawal;
import com.cryptotrading.repository.WithdrawalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalServiceImpl implements WithdrawalService {

    private final WithdrawalRepository withdrawalRepository;
    private final WalletService walletService;

    @Override
    @Transactional
    public Withdrawal requestWithdrawal(BigDecimal amount, User user) {
        validateAmount(amount);
        if (user == null || user.getId() == null) throw new IllegalArgumentException("Invalid user");

        Wallet wallet = walletService.getUserWallet(user);
        walletService.withdraw(wallet, amount); // Handles the balance check and deduction

        Withdrawal withdrawal = Withdrawal.builder()
                .amount(amount)
                .user(user)
                .status(WithdrawalStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        Withdrawal saved = withdrawalRepository.save(withdrawal);
        log.info("User {} requested withdrawal of ${}. Withdrawal ID: {}", user.getId(), amount, saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public Withdrawal processWithdrawal(Long withdrawalId, boolean accept) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId).orElseThrow(() -> {
            log.warn("Process withdrawal failed: ID {} not found", withdrawalId);
            return new RuntimeException("Withdrawal not found with id: " + withdrawalId);
        });

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            log.warn("Process withdrawal failed: ID {} has already been processed", withdrawalId);
            throw new IllegalStateException("Withdrawal has already been processed");
        }

        withdrawal.setProcessedAt(Instant.now());

        if (accept) {
            withdrawal.setStatus(WithdrawalStatus.SUCCESS);
            log.info("Withdrawal ID {} approved for User {}", withdrawalId, withdrawal.getUser().getId());
        } else {
            withdrawal.setStatus(WithdrawalStatus.DECLINED);
            Wallet wallet = walletService.getUserWallet(withdrawal.getUser());
            walletService.addBalance(wallet, withdrawal.getAmount());
            log.info("Withdrawal ID {} declined for User {}. ${} refunded to wallet.", withdrawalId, withdrawal.getUser().getId(), withdrawal.getAmount());
        }

        return withdrawalRepository.save(withdrawal);
    }

    @Override
    public List<Withdrawal> getUsersWithdrawalHistory(User user) {
        if (user == null || user.getId() == null) throw new IllegalArgumentException("Invalid user");
        return withdrawalRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Override
    public List<Withdrawal> getAllWithdrawalRequest() {
        return withdrawalRepository.findByStatusOrderByCreatedAtAsc(WithdrawalStatus.PENDING);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero");
        }
    }
}