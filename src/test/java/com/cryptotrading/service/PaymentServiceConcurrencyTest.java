package com.cryptotrading.service;

import com.cryptotrading.domain.PaymentMethod;
import com.cryptotrading.domain.PaymentOrderStatus;
import com.cryptotrading.model.PaymentOrder;
import com.cryptotrading.model.User;
import com.cryptotrading.model.Wallet;
import com.cryptotrading.repository.PaymentOrderRepository;
import com.cryptotrading.repository.UserRepository;
import com.cryptotrading.repository.WalletRepository;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

@SpringBootTest
public class PaymentServiceConcurrencyTest {

    @MockitoBean
    private LettuceConnectionFactory lettuceConnectionFactory;

    @MockitoBean
    private ProxyManager<byte[]> proxyManager;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoSpyBean
    private PaymentServiceImpl paymentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Test
    void concurrentDepositCallbacks_creditWalletExactlyOnce() throws InterruptedException {
        int threadCount = 20; // <-- Your N value
        BigDecimal depositAmount = new BigDecimal("500.00");

        // 1. Seed a user, wallet, and PENDING payment order in H2
        User user = userRepository.save(User.builder()
                .fullName("Concurrency Tester")
                .email("concurrency@tradeforge.com")
                .mobile("9999999999")
                .password("encoded_pass")
                .build());

        walletRepository.save(Wallet.builder()
                .user(user)
                .balance(BigDecimal.ZERO)
                .reservedBalance(BigDecimal.ZERO)
                .build());

        PaymentOrder order = paymentOrderRepository.save(PaymentOrder.builder()
                .user(user)
                .amount(depositAmount)
                .chargedAmount(new BigDecimal("42000.00"))
                .currency("INR")
                .paymentMethod(PaymentMethod.RAZORPAY)
                .status(PaymentOrderStatus.PENDING)
                .walletCredited(false)
                .build());

        // 2. Stub external Razorpay HTTP call to return true
        doReturn(true).when(paymentService).processRazorpayPayment(any(PaymentOrder.class), anyString());

        // 3. Fire N threads at the exact same moment
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // All 10 threads start together
                    paymentService.processPaymentAndCreditWallet(user, order.getId(), "pay_concurrent_123");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Any unexpected failure will be caught by assertions below
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown(); // Release all 10 threads simultaneously
        doneLatch.await();
        executor.shutdown();

        // 4. Verify wallet was credited EXACTLY ONCE (500.00, NOT 5000.00)
        Wallet finalWallet = walletRepository.findByUserId(user.getId()).orElseThrow();
        PaymentOrder finalOrder = paymentOrderRepository.findById(order.getId()).orElseThrow();

        assertEquals(threadCount, successCount.get(), "All threads should complete idempotently");
        assertEquals(0, depositAmount.compareTo(finalWallet.getBalance()),
                "Expected balance 500.00 but got " + finalWallet.getBalance());
        assertTrue(finalOrder.isWalletCredited());
    }
}
