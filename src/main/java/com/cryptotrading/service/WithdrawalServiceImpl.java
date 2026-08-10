package com.cryptotrading.service;

import com.cryptotrading.domain.WithdrawalStatus;
import com.cryptotrading.model.User;
import com.cryptotrading.model.Withdrawal;
import com.cryptotrading.repository.WithdrawalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WithdrawalServiceImpl implements WithdrawalService{

    private final WithdrawalRepository withdrawalRepository;


    @Override
    public Withdrawal requestWithdrawal(Long amount, User user) {
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setAmount(amount);
        withdrawal.setUser(user);
        withdrawal.setStatus(WithdrawalStatus.PENDING);
        return withdrawalRepository.save(withdrawal);
    }

    @Override
    public Withdrawal processWithWithdrawal(Long withdrawalId, boolean accept) throws Exception {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() ->
                        new Exception("Withdrawal not found with id: " + withdrawalId));

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new Exception(
                    "Withdrawal has already been processed. Current status: "
                            + withdrawal.getStatus()
            );
        }

        withdrawal.setDate(LocalDateTime.now());

        if(accept) {
            withdrawal.setStatus(WithdrawalStatus.SUCCESS);
        }else {
            withdrawal.setStatus(WithdrawalStatus.DECLINE);

        }
        return withdrawalRepository.save(withdrawal);
    }

    @Override
    public List<Withdrawal> getUsersWithdrawalHistory(User user) {
        return withdrawalRepository.findByUserId(user.getId());
    }

    @Override
    public List<Withdrawal> getAllWithdrawalRequest() {
        return withdrawalRepository.findAll();
    }
}
