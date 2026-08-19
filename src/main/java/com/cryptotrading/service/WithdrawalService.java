package com.cryptotrading.service;

import com.cryptotrading.model.User;
import com.cryptotrading.model.Withdrawal;

import java.math.BigDecimal;
import java.util.List;

public interface WithdrawalService {

    Withdrawal requestWithdrawal(BigDecimal amount, User user);

    Withdrawal processWithdrawal(Long withdrawalId, boolean accept);

    List<Withdrawal> getUsersWithdrawalHistory(User user);

    List<Withdrawal> getAllWithdrawalRequest();
}
