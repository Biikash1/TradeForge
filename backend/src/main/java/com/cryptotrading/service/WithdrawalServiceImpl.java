package com.cryptotrading.service;

import com.cryptotrading.domain.WithdrawalStatus;
import com.cryptotrading.model.User;
import com.cryptotrading.model.Wallet;
import com.cryptotrading.model.Withdrawal;
import com.cryptotrading.repository.WithdrawalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WithdrawalServiceImpl implements WithdrawalService{

    private final WithdrawalRepository withdrawalRepository;
    private final WalletService walletService;

    @Override
    @Transactional
    public Withdrawal requestWithdrawal(BigDecimal amount, User user) {

        validateAmount(amount);

        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException(
                    "Invalid user"
            );
        }

            Wallet wallet = walletService.getUserWallet(user);

            walletService.withdraw(wallet, amount);

            Withdrawal withdrawal = Withdrawal.builder()
                    .amount(amount)
                    .user(user)
                    .status(WithdrawalStatus.PENDING)
                    .createdAt(Instant.now())
                    .build();

        return withdrawalRepository.save(withdrawal);
    }

    @Override
    @Transactional
    public Withdrawal processWithdrawal(Long withdrawalId, boolean accept) {
            Withdrawal withdrawal =
                withdrawalRepository.findById(withdrawalId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Withdrawal not found with id: "
                                                + withdrawalId
                                )
                        );

            if (withdrawal.getStatus()
                    != WithdrawalStatus.PENDING) {

                throw new IllegalStateException(
                        "Withdrawal has already been processed"
                );
            }

            withdrawal.setProcessedAt(Instant.now());

            if (accept) {
                withdrawal.setStatus(
                        WithdrawalStatus.SUCCESS
                );
            } else {
                withdrawal.setStatus(
                        WithdrawalStatus.DECLINED
                );

                Wallet wallet =
                        walletService.getUserWallet(
                                withdrawal.getUser()
                        );

                walletService.addBalance(
                        wallet,
                        withdrawal.getAmount()
                );
            }

            return withdrawalRepository.save(withdrawal);
    }

    @Override
    public List<Withdrawal> getUsersWithdrawalHistory(User user) {

            if (user == null || user.getId() == null) {
                throw new IllegalArgumentException(
                        "Invalid user"
                );
            }

            return withdrawalRepository
                    .findByUserIdOrderByCreatedAtDesc(
                            user.getId()
                    );
    }

    @Override
    public List<Withdrawal> getAllWithdrawalRequest() {
            return withdrawalRepository
                    .findByStatusOrderByCreatedAtAsc(
                            WithdrawalStatus.PENDING
                    );
    }

        private void validateAmount(BigDecimal amount) {

            if (amount == null) {
                throw new IllegalArgumentException(
                        "Withdrawal amount cannot be null"
                );
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Withdrawal amount must be greater than zero"
                );
            }
        }
}
