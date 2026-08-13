package com.cryptotrading.service;

import com.cryptotrading.domain.PaymentMethod;
import com.cryptotrading.domain.PaymentOrderStatus;
import com.cryptotrading.dto.PaymentResponse;
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
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.expression.ExpressionException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService{

     private final PaymentOrderRepository paymentOrderRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${stripe.key.id}")
    private String stripeKeyId;

    @Value("${stripe.key.secret}")
    private String stripeKeySecret;

    @Override
    public PaymentOrder createOrder(User user, Long amount, PaymentMethod paymentMethod) {
       PaymentOrder paymentOrder = new PaymentOrder();
       paymentOrder.setUser(user);
       paymentOrder.setAmount(amount);
       paymentOrder.setPaymentMethod(paymentMethod);
       return paymentOrderRepository.save(paymentOrder);
    }

    @Override
    public PaymentOrder getPaymentOrderById(Long id) throws Exception {
        return paymentOrderRepository.findById(id)
                .orElseThrow(() ->
                        new Exception("Payment order not found")
                        );
    }

    @Override
    public Boolean ProccedPaymentOrder(PaymentOrder paymentOrder, String paymentId) throws RazorpayException {
       if(paymentOrder.getStatus().equals(PaymentOrderStatus.PENDING)) {
           if(paymentOrder.getPaymentMethod().equals(PaymentMethod.RAZORPAY)) {
               RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
               Payment payment = razorpay.payments.fetch(paymentId);

               Integer amount = payment.get("amount");
               String status = payment.get("status");

               if(status.equals("captured")) {
                   paymentOrder.setStatus(PaymentOrderStatus.SUCCESS);
                   return true;
               }
               paymentOrder.setStatus(PaymentOrderStatus.FAILED);
               paymentOrderRepository.save(paymentOrder);
               return false;
           }

           paymentOrder.setStatus(PaymentOrderStatus.SUCCESS);
           paymentOrderRepository.save(paymentOrder);
           return true;
       }
       return false;
    }

    @Override
    public PaymentResponse createRazorpayPaymentLink(User user, Long amount) {

         Long Amount =  amount * 100;

         try {
             //Instantiate a Razorpay client with your key ID and SECRET
             RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

             //Create a JSON object with th payment link request parameter
             JSONObject paymentLinkRequest = new JSONObject();
             paymentLinkRequest.put("amount", amount);
             paymentLinkRequest.put("currency", "INR");

             //Create a JSON object with the customer details
             JSONObject customer = new JSONObject();
             customer.put("name", user.getFullName());

             customer.put("email", user.getEmail());
             paymentLinkRequest.put("customer", customer);

             //Create a JSON object with the notification settings
             JSONObject notify = new JSONObject();
             notify.put("email", true);
             paymentLinkRequest.put("notify", notify);

             //set the remainder setting
             paymentLinkRequest.put("remainder_enable", true);

             //set the callback URL and method
             paymentLinkRequest.put("callback_url", "http://localhost:5173/wallet");
             paymentLinkRequest.put("callback_method", "get");

             //Create the payment link using the paymentLink.create() method
             PaymentLink paymentLink = razorpay.paymentLink.create(paymentLinkRequest);

             String paymentLinkId = paymentLink.get("id");
             String paymentLinkUrl = paymentLink.get("short_url");

             PaymentResponse response = new PaymentResponse();
             response.setPayment_url(paymentLinkUrl);
             return response;
         } catch (RazorpayException e) {
             System.out.println("Error creating payment link: " +e.getMessage());
             throw new RuntimeException(e.getMessage());
         }
    }

    @Override
    public PaymentResponse createStripePaymentLink(User user, Long amount, Long orderId) throws StripeException {
        Stripe.stripekeyId = stripeKeySecret;

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("http://localhost:5713/wallet?order_id="+orderId)
                .setCancelUrl("http://localhost:5713/payment/cancel")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(
                                SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency("usd")
                                        .setUnitAmount(amount*100)
                                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                .setName("Top up wallet")
                                                .build()
                                        ).build()
                        ).build()

                ).build();

        Session session = Session.create(params);
        System.out.println("Session ______" +session);

        PaymentResponse response = new PaymentResponse();
        response.setPayment_url(session.getUrl());
        return response;
    }
}
