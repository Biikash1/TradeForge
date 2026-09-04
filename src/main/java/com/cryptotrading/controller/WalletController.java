package com.cryptotrading.controller;

import com.cryptotrading.dto.WalletResponse;
import com.cryptotrading.dto.WalletTransferRequest;
import com.cryptotrading.model.Order;
import com.cryptotrading.model.User;
import com.cryptotrading.model.Wallet;
import com.cryptotrading.model.WalletTransaction;
import com.cryptotrading.service.OrderService;
import com.cryptotrading.service.PaymentService;
import com.cryptotrading.service.UserService;
import com.cryptotrading.service.WalletService;
import com.cryptotrading.service.WalletTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final UserService userService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final WalletTransactionService walletTransactionService; // Added

    // 1. Get authenticated user's wallet
    @GetMapping
    public ResponseEntity<WalletResponse> getUserWallet(
            @RequestHeader("Authorization") String jwt) {

        User user = userService.findUserProfileByJwt(jwt);
        Wallet wallet = walletService.getUserWallet(user);

        return ResponseEntity.ok(WalletResponse.from(wallet));
    }

    // 2. Get wallet transaction history (Matches GET /api/wallet/transaction)
    @GetMapping("/transaction")
    public ResponseEntity<List<WalletTransaction>> getWalletTransactions(
            @RequestHeader("Authorization") String jwt) {

        User user = userService.findUserProfileByJwt(jwt);
        Wallet wallet = walletService.getUserWallet(user);

        List<WalletTransaction> transactions =
                walletTransactionService.getTransactionsByWallet(wallet);

        return ResponseEntity.ok(transactions != null ? transactions : List.of());
    }

    // 3. Transfer funds to another wallet
    @PostMapping("/{walletId}/transfer")
    public ResponseEntity<WalletResponse> transfer(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long walletId,
            @Valid @RequestBody WalletTransferRequest request) {

        User sender = userService.findUserProfileByJwt(jwt);
        Wallet receiverWallet = walletService.findWalletById(walletId);

        Wallet wallet = walletService.transfer(
                sender,
                receiverWallet,
                request.getAmount()
        );

        return ResponseEntity.ok(WalletResponse.from(wallet));
    }

    // 4. Pay for a trading order using wallet balance
    @PostMapping("/order/{orderId}/pay")
    public ResponseEntity<WalletResponse> payOrder(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long orderId) {

        User user = userService.findUserProfileByJwt(jwt);
        Order order = orderService.getOrderById(orderId);

        Wallet wallet = walletService.payOrder(order, user);

        return ResponseEntity.ok(WalletResponse.from(wallet));
    }

    // 5. Verify external payment and credit wallet
    @PostMapping("/deposit")
    public ResponseEntity<WalletResponse> addBalanceToWallet(
            @RequestHeader("Authorization") String jwt,
            @RequestParam(name = "order_id") Long orderId,
            @RequestParam(name = "payment_id") String paymentId) {

        User user = userService.findUserProfileByJwt(jwt);

        Wallet wallet = paymentService.processPaymentAndCreditWallet(
                user,
                orderId,
                paymentId
        );

        return ResponseEntity.ok(WalletResponse.from(wallet));
    }
}