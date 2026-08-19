package com.cryptotrading.service;

import com.cryptotrading.domain.OrderType;
import com.cryptotrading.model.Order;
import com.cryptotrading.model.User;
import com.cryptotrading.model.Wallet;
import com.cryptotrading.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements  WalletService{

    private final WalletRepository walletRepository;

    @Override
    @Transactional
    public Wallet getUserWallet(User user) {

        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Invalid user");
        }

        return walletRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Wallet wallet = Wallet.builder()
                            .user(user)
                            .balance(BigDecimal.ZERO)
                            .build();

                    return walletRepository.save(wallet);
                });
    }

    @Override
    @Transactional
    public Wallet addBalance(Wallet wallet,  BigDecimal amount) {

        validateAmount(amount);

        if (wallet == null) {
            throw new IllegalArgumentException("Wallet cannot be null");
        }

        BigDecimal currentBalance = getBalance(wallet);

        wallet.setBalance(
                currentBalance.add(amount)
        );

        return walletRepository.save(wallet);
    }

    @Override
    public Wallet findWalletById(Long id)  {
        if (id == null) {
            throw new IllegalArgumentException("Wallet ID cannot be null");
        }

        return walletRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Wallet not found with id: " + id)
                );
    }


   @Override
    @Transactional
    public Wallet transfer(User sender, Wallet receiverWallet,  BigDecimal amount) {
             validateAmount(amount);

        if (sender == null) {
            throw new IllegalArgumentException("Sender cannot be null");
        }

        if (receiverWallet == null) {
            throw new IllegalArgumentException(
                    "Receiver wallet cannot be null"
            );
        }

        Wallet senderWallet = getUserWallet(sender);

        if (senderWallet.getId().equals(receiverWallet.getId())) {
            throw new IllegalArgumentException(
                    "Sender and receiver wallets cannot be the same"
            );
        }

        BigDecimal senderBalance = getBalance(senderWallet);

        if (senderBalance.compareTo(amount) < 0) {
            throw new IllegalStateException(
                    "Insufficient wallet balance"
            );
        }

        senderWallet.setBalance(
                senderBalance.subtract(amount)
        );

        BigDecimal receiverBalance = getBalance(receiverWallet);

        receiverWallet.setBalance(
                receiverBalance.add(amount)
        );

        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        return senderWallet;
    }

    @Override
    public Wallet payOrder(Order order, User user){

        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        BigDecimal orderPrice = order.getPrice();

        validateAmount(orderPrice);

        Wallet wallet = getUserWallet(user);

        BigDecimal currentBalance = getBalance(wallet);

        if (order.getOrderType() == OrderType.BUY) {

            if (currentBalance.compareTo(orderPrice) < 0) {
                throw new IllegalStateException(
                        "Insufficient funds for this transaction"
                );
            }

            wallet.setBalance(
                    currentBalance.subtract(orderPrice)
            );

        } else if (order.getOrderType() == OrderType.SELL) {

            wallet.setBalance(
                    currentBalance.add(orderPrice)
            );

        } else {
            throw new IllegalArgumentException(
                    "Unsupported order type: " + order.getOrderType()
            );
        }

        return walletRepository.save(wallet);
    }

    @Override
    @Transactional
    public Wallet withdraw(Wallet wallet, BigDecimal amount) {

        validateAmount(amount);

        if (wallet == null) {
            throw new IllegalArgumentException(
                    "Wallet cannot be null"
            );
        }

        BigDecimal currentBalance = wallet.getBalance();

        if (currentBalance == null) {
            currentBalance = BigDecimal.ZERO;
        }

        if (currentBalance.compareTo(amount) < 0) {
            throw new IllegalStateException(
                    "Insufficient wallet balance"
            );
        }

        wallet.setBalance(
                currentBalance.subtract(amount)
        );

        return walletRepository.save(wallet);
    }

    private BigDecimal getBalance(Wallet wallet) {

        return wallet.getBalance() != null
                ? wallet.getBalance()
                : BigDecimal.ZERO;
    }

    private void validateAmount(BigDecimal amount) {

        if (amount == null) {
            throw new IllegalArgumentException(
                    "Amount cannot be null"
            );
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero"
            );
        }
    }

}
