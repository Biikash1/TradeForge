package com.cryptotrading.service;

import com.cryptotrading.dto.PaymentDetailsRequest;
import com.cryptotrading.exception.ResourceNotFoundException;
import com.cryptotrading.model.PaymentDetails;
import com.cryptotrading.model.User;
import com.cryptotrading.repository.PaymentDetailsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentDetailsServiceImpl implements PaymentDetailsService {

    private final PaymentDetailsRepository paymentDetailsRepository;

    @Override
    @Transactional
    public PaymentDetails addPaymentDetails(PaymentDetailsRequest request, User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }

        // Upsert: Retrieve existing details or build a new record
        PaymentDetails paymentDetails = paymentDetailsRepository.findByUserId(user.getId())
                .orElseGet(() -> PaymentDetails.builder()
                        .user(user)
                        .build());

        paymentDetails.setAccountNumber(request.getAccountNumber().trim());
        paymentDetails.setAccountHolderName(request.getAccountHolderName().trim());
        paymentDetails.setIfsc(request.getIfsc().trim().toUpperCase());
        paymentDetails.setBankName(request.getBankName().trim());

        return paymentDetailsRepository.save(paymentDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDetails getUsersPaymentDetails(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        return paymentDetailsRepository
                .findByUserId(user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment details not found for user: " + user.getId()
                        )
                );
    }
}