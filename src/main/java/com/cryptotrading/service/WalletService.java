package com.cryptotrading.service;

import com.cryptotrading.model.Order;
import com.cryptotrading.model.User;
import com.cryptotrading.model.Wallet;

import java.math.BigDecimal;

public interface WalletService {

    Wallet getUserWallet(User user);

    Wallet addBalance(Wallet wallet, BigDecimal amount);

    Wallet withdraw(Wallet wallet, BigDecimal amount);

    Wallet findWalletById(Long id);

    Wallet transfer(User sender, Wallet receiverWallet, BigDecimal amount);

    Wallet payOrder(Order order, User user) ;

}
