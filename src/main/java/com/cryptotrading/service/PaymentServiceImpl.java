package com.cryptotrading.service;

import com.cryptotrading.domain.PaymentMethod;
import com.cryptotrading.domain.PaymentOrderStatus;
import com.cryptotrading.dto.PaymentResponse;
import com.cryptotrading.exception.*;
import com.cryptotrading.model.PaymentOrder;
import com.cryptotrading.model.User;
import com.cryptotrading.model.Wallet;
import com.cryptotrading.repository.PaymentOrderRepository;
import com.razorpay.Payment;
import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService{

     private final PaymentOrderRepository paymentOrderRepository;
     private final WalletService walletService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.callback.url}")
    private String razorpayCallbackUrl;

    @Value("${stripe.key.id}")
    private String stripeKeyId;

    @Value("${stripe.key.secret}")
    private String stripeKeySecret;

    @Value("${stripe.success.url}")
    private String stripeSuccessUrl;

    @Value("${stripe.cancel.url}")
    private String stripeCancelUrl;

    @Override
    @Transactional
    public PaymentOrder createOrder(User user,
                                    BigDecimal amount,
                                    PaymentMethod paymentMethod) {
        validateAmount(amount);

        if (user == null) {
            throw new InvalidPaymentException("User is required");
        }

        if (paymentMethod == null) {
            throw new InvalidPaymentException(
                    "Payment method is required"
            );
        }

        PaymentOrder paymentOrder = PaymentOrder.builder()
                .user(user)
                .amount(amount.setScale(2, RoundingMode.HALF_UP))
                .paymentMethod(paymentMethod)
                .status(PaymentOrderStatus.PENDING)
                .build();

        return paymentOrderRepository.save(paymentOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentOrder getPaymentOrderById(Long id){

        return paymentOrderRepository.findById(id)
                .orElseThrow(() ->
                        new PaymentOrderNotFoundException(
                                "Payment order not found: " + id
                        )
                );
    }

    @Override
    @Transactional
    public boolean processRazorpayPayment(PaymentOrder paymentOrder,
                                       String paymentId) {

        if (paymentOrder == null) {
            throw new InvalidPaymentException(
                    "Payment order cannot be null"
            );
        }

        if (paymentId == null || paymentId.isBlank()) {
            throw new InvalidPaymentException(
                    "Payment ID is required"
            );
        }

        if (paymentOrder.getStatus() == PaymentOrderStatus.SUCCESS) {
            return true;
        }

        if (paymentOrder.getStatus() != PaymentOrderStatus.PENDING) {
            return false;
        }

        if (paymentOrder.getPaymentMethod() != PaymentMethod.RAZORPAY) {
            throw new InvalidPaymentException(
                    "Payment order is not a Razorpay order"
            );
        }

        try {

            RazorpayClient razorpayClient =
                    new RazorpayClient(
                            razorpayKeyId,
                            razorpayKeySecret
                    );

            Payment payment =
                    razorpayClient.payments.fetch(paymentId);

            String providerStatus = payment.get("status");
            Integer providerAmount = payment.get("amount");
            String providerCurrency = payment.get("currency");

            long expectedAmountInPaise =
                    paymentOrder.getAmount()
                            .movePointRight(2)
                            .longValueExact();

            // Verify amount
            if (providerAmount == null
                    || providerAmount.longValue()
                    != expectedAmountInPaise) {

                markPaymentFailed(paymentOrder, paymentId);

                log.warn(
                        "Razorpay amount mismatch. orderId={}, expected={}, actual={}",
                        paymentOrder.getId(),
                        expectedAmountInPaise,
                        providerAmount
                );

                throw new PaymentVerificationException(
                        "Razorpay payment amount verification failed"
                );
            }

            // Verify currency
            if (!"INR".equalsIgnoreCase(providerCurrency)) {

                markPaymentFailed(paymentOrder, paymentId);

                throw new PaymentVerificationException(
                        "Razorpay payment currency verification failed"
                );
            }

            // Verify payment status
            if (!"captured".equalsIgnoreCase(providerStatus)) {

                markPaymentFailed(paymentOrder, paymentId);

                throw new PaymentVerificationException(
                        "Razorpay payment was not captured"
                );
            }

            paymentOrder.setStatus(
                    PaymentOrderStatus.SUCCESS
            );

            paymentOrder.setProviderPaymentId(
                    paymentId
            );

            paymentOrderRepository.save(paymentOrder);

            log.info(
                    "Razorpay payment successful. orderId={}, paymentId={}",
                    paymentOrder.getId(),
                    paymentId
            );

            return true;
        } catch (RazorpayException e) {

            log.error(
                    "Razorpay verification failed. orderId={}, paymentId={}",
                    paymentOrder.getId(),
                    paymentId,
                    e
            );

            throw new PaymentVerificationException(
                    "Unable to verify Razorpay payment",
                    e
            );
        }

    }

    @Override
    public boolean processStripePayment(PaymentOrder paymentOrder, String sessionId) {
        if (paymentOrder == null) {
            throw new InvalidPaymentException(
                    "Payment order cannot be null"
            );
        }

        if (sessionId == null || sessionId.isBlank()) {
            throw new InvalidPaymentException(
                    "Stripe session ID is required"
            );
        }

        if (paymentOrder.getStatus() == PaymentOrderStatus.SUCCESS) {
            return true;
        }

        if (paymentOrder.getStatus() != PaymentOrderStatus.PENDING) {
            return false;
        }

        if (paymentOrder.getPaymentMethod() != PaymentMethod.STRIPE) {
            throw new InvalidPaymentException(
                    "Payment order is not a Stripe order"
            );
        }

        try {

            Stripe.apiKey = stripeKeySecret;

            Session session =
                    Session.retrieve(sessionId);

            String orderId =
                    session.getMetadata() != null
                            ? session.getMetadata().get("order_id")
                            : null;

            // Verify internal order
            if (!String.valueOf(paymentOrder.getId())
                    .equals(orderId)) {

                throw new PaymentVerificationException(
                        "Stripe session does not belong to this payment order"
                );
            }

            // Verify payment amount
            long expectedAmountInCents =
                    paymentOrder.getAmount()
                            .movePointRight(2)
                            .longValueExact();

            Long stripeAmount =
                    session.getAmountTotal();

            if (stripeAmount == null
                    || stripeAmount != expectedAmountInCents) {

                markPaymentFailed(
                        paymentOrder,
                        sessionId
                );

                throw new PaymentVerificationException(
                        "Stripe payment amount verification failed"
                );
            }

            // Verify currency
            String currency =
                    session.getCurrency();

            if (!"usd".equalsIgnoreCase(currency)) {

                markPaymentFailed(
                        paymentOrder,
                        sessionId
                );

                throw new PaymentVerificationException(
                        "Stripe payment currency verification failed"
                );
            }

            // Verify payment status
            if (!"paid".equalsIgnoreCase(
                    session.getPaymentStatus())) {

                markPaymentFailed(
                        paymentOrder,
                        sessionId
                );

                throw new PaymentVerificationException(
                        "Stripe payment has not been completed"
                );
            }

            paymentOrder.setStatus(
                    PaymentOrderStatus.SUCCESS
            );

            paymentOrder.setProviderPaymentId(
                    sessionId
            );

            paymentOrderRepository.save(paymentOrder);

            log.info(
                    "Stripe payment successful. orderId={}, sessionId={}",
                    paymentOrder.getId(),
                    sessionId
            );

            return true;

        } catch (StripeException e) {

            log.error(
                    "Stripe verification failed. orderId={}, sessionId={}",
                    paymentOrder.getId(),
                    sessionId,
                    e
            );

            throw new PaymentVerificationException(
                    "Unable to verify Stripe payment",
                    e
            );
        }
    }

    @Override
    public PaymentResponse createRazorpayPaymentLink(User user,
                                                     BigDecimal amount,
                                                     Long orderId) {

        if (user == null) {
            throw new InvalidPaymentException("User is required");
        }

        if (orderId == null) {
            throw new InvalidPaymentException("Order ID is required");
        }

        validateAmount(amount);

        long amountInPaise;

        try {
            amountInPaise = amount
                    .setScale(2)
                    .movePointRight(2)
                    .longValueExact();
        } catch (ArithmeticException e) {
            throw new InvalidPaymentException(
                    "Invalid payment amount"
            );
        }

        try {
            RazorpayClient razorpayClient =
                    new RazorpayClient(
                            razorpayKeyId,
                            razorpayKeySecret
                    );

            JSONObject request = new JSONObject();

            request.put("amount", amountInPaise);
            request.put("currency", "INR");
            request.put(
                    "reference_id",
                    String.valueOf(orderId)
            );
            request.put(
                    "description",
                    "TradeForge wallet top-up"
            );

            JSONObject customer = new JSONObject();
            customer.put("name", user.getFullName());
            customer.put("email", user.getEmail());

            request.put("customer", customer);

            JSONObject notify = new JSONObject();
            notify.put("email", true);

            request.put("notify", notify);
            request.put("reminder_enable", true);
            request.put(
                    "callback_url",
                    razorpayCallbackUrl
            );
            request.put("callback_method", "get");

            PaymentLink paymentLink =
                    razorpayClient.paymentLink.create(request);

            String paymentLinkId =
                    paymentLink.get("id");

            String paymentLinkUrl =
                    paymentLink.get("short_url");

            log.info(
                    "Razorpay payment link created. orderId={}, paymentLinkId={}",
                    orderId,
                    paymentLinkId
            );

            return PaymentResponse.builder()
                    .paymentId(paymentLinkId)
                    .paymentUrl(paymentLinkUrl)
                    .build();

        } catch (RazorpayException e) {

            log.error(
                    "Failed to create Razorpay payment link. orderId={}",
                    orderId,
                    e
            );

            throw new PaymentVerificationException(
                    "Unable to create Razorpay payment link",
                    e
            );
        }
    }


    @Override
    public PaymentResponse createStripePaymentLink(User user, BigDecimal amount, Long orderId) {

        if (user == null) {
            throw new InvalidPaymentException("User is required");
        }

        if (orderId == null) {
            throw new InvalidPaymentException("Order ID is required");
        }

        validateAmount(amount);

        long amountInCents;

        try {
            amountInCents = amount
                    .setScale(2)
                    .movePointRight(2)
                    .longValueExact();
        } catch (ArithmeticException e) {
            throw new InvalidPaymentException(
                    "Invalid payment amount"
            );
        }
        try {

            // Stripe secret key
            Stripe.apiKey = stripeKeySecret;

            SessionCreateParams params =
                    SessionCreateParams.builder()
                            .setMode(
                                    SessionCreateParams.Mode.PAYMENT
                            )
                            .setSuccessUrl(
                                    stripeSuccessUrl
                                            + "?order_id="
                                            + orderId
                            )
                            .setCancelUrl(
                                    stripeCancelUrl
                            )
                            .addPaymentMethodType(
                                    SessionCreateParams
                                            .PaymentMethodType.CARD
                            )
                            .addLineItem(
                                    SessionCreateParams.LineItem
                                            .builder()
                                            .setQuantity(1L)
                                            .setPriceData(
                                                    SessionCreateParams
                                                            .LineItem
                                                            .PriceData
                                                            .builder()
                                                            .setCurrency("usd")
                                                            .setUnitAmount(
                                                                    amountInCents
                                                            )
                                                            .setProductData(
                                                                    SessionCreateParams
                                                                            .LineItem
                                                                            .PriceData
                                                                            .ProductData
                                                                            .builder()
                                                                            .setName(
                                                                                    "TradeForge Wallet Top Up"
                                                                            )
                                                                            .build()
                                                            )
                                                            .build()
                                            )
                                            .build()
                            )
                            .putMetadata(
                                    "order_id",
                                    String.valueOf(orderId)
                            )
                            .build();

            Session session =
                    Session.create(params);

            log.info(
                    "Stripe checkout session created. orderId={}, sessionId={}",
                    orderId,
                    session.getId()
            );

            return PaymentResponse.builder()
                    .paymentId(session.getId())
                    .paymentUrl(session.getUrl())
                    .build();

        } catch (StripeException e) {

            log.error(
                    "Failed to create Stripe checkout session. orderId={}",
                    orderId,
                    e
            );

            throw new PaymentVerificationException(
                    "Unable to create Stripe checkout session",
                    e
            );
        }
    }


    @Override
    @Transactional
    public Wallet processPaymentAndCreditWallet(
            User user,
            Long orderId,
            String paymentId
    ) {

        if (user == null) {
            throw new InvalidPaymentException(
                    "User cannot be null"
            );
        }

        if (orderId == null) {
            throw new InvalidPaymentException(
                    "Order ID is required"
            );
        }

        if (paymentId == null || paymentId.isBlank()) {
            throw new InvalidPaymentException(
                    "Payment ID is required"
            );
        }

        PaymentOrder paymentOrder =
                paymentOrderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new PaymentOrderNotFoundException(
                                        "Payment order not found"
                                )
                        );

        // Verify ownership
        if (!paymentOrder.getUser().getId()
                .equals(user.getId())) {

            throw new PaymentOwnershipException(
                    "Payment order does not belong to the authenticated user"
            );
        }

        // Idempotency check
        if (paymentOrder.isWalletCredited()) {
            return walletService.getUserWallet(user);
        }

        // Verify payment with provider
        boolean paymentSuccessful;

        if (paymentOrder.getPaymentMethod()
                == PaymentMethod.RAZORPAY) {

            paymentSuccessful =
                    processRazorpayPayment(
                            paymentOrder,
                            paymentId
                    );

        } else if (paymentOrder.getPaymentMethod()
                == PaymentMethod.STRIPE) {

            paymentSuccessful =
                    processStripePayment(
                            paymentOrder,
                            paymentId
                    );

        } else {
            throw new InvalidPaymentException(
                    "Unsupported payment method: "
                            + paymentOrder.getPaymentMethod()
            );
        }

        if (!paymentSuccessful) {
            throw new PaymentVerificationException(
                    "Payment verification failed for order: "
                            + orderId
            );
        }

        // Get wallet
        Wallet wallet =
                walletService.getUserWallet(user);

        // Credit wallet
        wallet = walletService.addBalance(
                wallet,
                paymentOrder.getAmount()
        );

        // Mark payment as credited
        paymentOrder.setWalletCredited(true);

        paymentOrderRepository.save(paymentOrder);

        return wallet;
    }

    private void validateAmount(BigDecimal amount) {

        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new InvalidPaymentException(
                    "Payment amount must be greater than zero"
            );
        }

        if (amount.scale() > 2) {
            throw new InvalidPaymentException(
                    "Payment amount cannot have more than 2 decimal places"
            );
        }
    }

    private void markPaymentFailed(
            PaymentOrder paymentOrder,
            String paymentId
    ) {

        paymentOrder.setStatus(
                PaymentOrderStatus.FAILED
        );

        paymentOrder.setProviderPaymentId(
                paymentId
        );

        paymentOrderRepository.save(paymentOrder);
    }

}
