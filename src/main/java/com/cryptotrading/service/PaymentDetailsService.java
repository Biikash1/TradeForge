package com.cryptotrading.service;

import com.cryptotrading.dto.PaymentDetailsRequest;
import com.cryptotrading.model.PaymentDetails;
import com.cryptotrading.model.User;
import java.util.List;

public interface PaymentDetailsService {
    PaymentDetails addPaymentDetails(PaymentDetailsRequest request, User user);
    List<PaymentDetails> getUsersPaymentDetails(User user);
    void deletePaymentDetails(Long id, User user);
}