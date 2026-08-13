package com.cryptotrading.service;

import com.cryptotrading.domain.PaymentMethod;
import com.cryptotrading.domain.PaymentOrderStatus;
import com.cryptotrading.dto.PaymentResponse;
import com.cryptotrading.exception.ResourceNotFoundException;
import com.cryptotrading.model.PaymentOrder;
import com.cryptotrading.model.User;
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

        if (paymentMethod == null) {
            throw new IllegalArgumentException(
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
                        new ResourceNotFoundException(
                                "Payment order not found: " + id
                        )
                );
    }

    @Override
    @Transactional
    public boolean processRazorpayPayment(PaymentOrder paymentOrder,
                                       String paymentId) throws RazorpayException {

        if (paymentOrder == null) {
            throw new IllegalArgumentException(
                    "Payment order cannot be null"
            );
        }

        if (paymentId == null || paymentId.isBlank()) {
            throw new IllegalArgumentException(
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
            throw new IllegalStateException(
                    "Payment order is not a Razorpay order"
            );
        }

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

        if (providerAmount == null
                || providerAmount.longValue() != expectedAmountInPaise) {

            markPaymentFailed(paymentOrder, paymentId);

            log.warn(
                    "Razorpay amount mismatch. orderId={}, expected={}, actual={}",
                    paymentOrder.getId(),
                    expectedAmountInPaise,
                    providerAmount
            );

            return false;
        }

        if (!"INR".equalsIgnoreCase(providerCurrency)) {

            markPaymentFailed(paymentOrder, paymentId);

            log.warn(
                    "Razorpay currency mismatch. orderId={}, currency={}",
                    paymentOrder.getId(),
                    providerCurrency
            );

            return false;
        }

        if ("captured".equalsIgnoreCase(providerStatus)) {

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
        }

        markPaymentFailed(paymentOrder, paymentId);

        log.warn(
                "Razorpay payment failed. orderId={}, paymentId={}, status={}",
                paymentOrder.getId(),
                paymentId,
                providerStatus
        );

        return false;

    }

    @Override
    public boolean processStripePayment(PaymentOrder paymentOrder, String sessionId) throws StripeException {
        if (paymentOrder == null) {
            throw new IllegalArgumentException(
                    "Payment order cannot be null"
            );
        }

        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException(
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
            throw new IllegalStateException(
                    "Payment order is not a Stripe order"
            );
        }

        Stripe.apiKey = stripeKeySecret;

        Session session = Session.retrieve(sessionId);

        String paymentStatus = session.getPaymentStatus();

        String orderId =
                session.getMetadata()
                        .get("order_id");

        if (!String.valueOf(paymentOrder.getId())
                .equals(orderId)) {

            throw new IllegalStateException(
                    "Stripe session does not belong to this payment order"
            );
        }

        if ("paid".equalsIgnoreCase(paymentStatus)) {

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
        }

        markPaymentFailed(
                paymentOrder,
                sessionId
        );

        return false;
    }

    @Override
    public PaymentResponse createRazorpayPaymentLink(User user, BigDecimal amount, Long orderId)
            throws RazorpayException {

        validateAmount(amount);

        long amountInPaise =
                amount
                        .movePointRight(2)
                        .longValueExact();

        RazorpayClient razorpayClient =
                new RazorpayClient(
                        razorpayKeyId,
                        razorpayKeySecret
                );

        JSONObject paymentLinkRequest = new JSONObject();

        paymentLinkRequest.put(
                "amount",
                amountInPaise
        );

        paymentLinkRequest.put(
                "currency",
                "INR"
        );

        paymentLinkRequest.put(
                "reference_id",
                String.valueOf(orderId)
        );

        paymentLinkRequest.put(
                "description",
                "TradeForge wallet top-up"
        );

        JSONObject customer =
                new JSONObject();

        customer.put(
                "name",
                user.getFullName()
        );

        customer.put(
                "email",
                user.getEmail()
        );

        paymentLinkRequest.put(
                "customer",
                customer
        );

        JSONObject notify =
                new JSONObject();

        notify.put(
                "email",
                true
        );

        paymentLinkRequest.put(
                "notify",
                notify
        );

        paymentLinkRequest.put(
                "reminder_enable",
                true
        );

        paymentLinkRequest.put(
                "callback_url",
                razorpayCallbackUrl
        );

        paymentLinkRequest.put(
                "callback_method",
                "get"
        );

        PaymentLink paymentLink =
                razorpayClient.paymentLink
                        .create(paymentLinkRequest);

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
    }


    @Override
    public PaymentResponse createStripePaymentLink(User user, BigDecimal amount, Long orderId) throws StripeException {
        validateAmount(amount);

        /*
         * Stripe secret key.
         */
        Stripe.apiKey = stripeKeySecret;

        long amountInCents =
                amount
                        .movePointRight(2)
                        .longValueExact();

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

                        /*
                         * Associate Stripe session with
                         * our internal payment order.
                         */
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
    }

    private void validateAmount(BigDecimal amount) {

        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero"
            );
        }

        if (amount.scale() > 2) {
            throw new IllegalArgumentException(
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
