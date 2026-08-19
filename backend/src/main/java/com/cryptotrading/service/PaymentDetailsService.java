package com.cryptotrading.service;

import com.cryptotrading.dto.PaymentDetailsRequest;
import com.cryptotrading.model.PaymentDetails;
import com.cryptotrading.model.User;

public interface PaymentDetailsService {

    PaymentDetails addPaymentDetails(
            PaymentDetailsRequest request,
            User user);

     PaymentDetails getUsersPaymentDetails(User user);
}
