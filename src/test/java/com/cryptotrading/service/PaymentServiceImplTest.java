package com.cryptotrading.service;

import com.cryptotrading.domain.PaymentMethod;
import com.cryptotrading.domain.PaymentOrderStatus;
import com.cryptotrading.exception.PaymentOwnershipException;
import com.cryptotrading.model.PaymentOrder;
import com.cryptotrading.model.User;
import com.cryptotrading.model.Wallet;
import com.cryptotrading.repository.PaymentOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceImplTest {

    @Mock
    private PaymentOrderRepository paymentOrderRepository;

    @Mock
    private WalletService walletService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userA = new User();
        userA.setId(1L);

        userB = new User();
        userB.setId(2L);
    }

    @Test
    void createOrder_SuccessfullyCreatesPendingOrder() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(500.00);
        PaymentOrder mockSavedOrder = PaymentOrder.builder()
                .id(10L)
                .amount(amount)
                .status(PaymentOrderStatus.PENDING)
                .build();

        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenReturn(mockSavedOrder);

        // Act
        PaymentOrder result = paymentService.createOrder(userA, amount, PaymentMethod.RAZORPAY);

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.getId());
        verify(paymentOrderRepository, times(1)).save(any(PaymentOrder.class));
    }

    @Test
    void processPaymentAndCreditWallet_ThrowsExceptionOnWrongUser() {
        // Arrange: Order belongs to user A
        PaymentOrder order = PaymentOrder.builder()
                .id(100L)
                .user(userA)
                .build();

        when(paymentOrderRepository.findById(100L)).thenReturn(Optional.of(order));

        // Act & Assert: User B tries to process User A's order
        assertThrows(PaymentOwnershipException.class, () -> {
            paymentService.processPaymentAndCreditWallet(userB, 100L, "pay_123");
        });
    }

    @Test
    void processPaymentAndCreditWallet_Idempotency_ReturnsWalletIfAlreadyCredited() {
        // Arrange: Order belongs to user A, but walletCredited is already true
        PaymentOrder order = PaymentOrder.builder()
                .id(100L)
                .user(userA)
                .walletCredited(true)
                .build();

        Wallet mockWallet = new Wallet();
        mockWallet.setBalance(BigDecimal.valueOf(1000));

        when(paymentOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(walletService.getUserWallet(userA)).thenReturn(mockWallet);

        // Act
        Wallet resultWallet = paymentService.processPaymentAndCreditWallet(userA, 100L, "pay_123");

        // Assert: Should return wallet directly WITHOUT hitting the Razorpay/Stripe APIs or saving to DB
        assertNotNull(resultWallet);
        assertEquals(BigDecimal.valueOf(1000), resultWallet.getBalance());

        // Verify we never attempted to add balance or save the order again
        verify(walletService, never()).addBalance(any(), any());
        verify(paymentOrderRepository, never()).save(any());
    }
}
