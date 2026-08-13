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
public class PaymentDetailsServiceImpl implements PaymentDetailsService{

    private final PaymentDetailsRepository paymentDetailsRepository;

    @Override
    @Transactional
    public PaymentDetails addPaymentDetails(
                                           PaymentDetailsRequest request,
                                            User user) {
        PaymentDetails paymentDetails = PaymentDetails.builder()
                .accountNumber(request.getAccountNumber().trim())
                .accountHolderName(request.getAccountHolderName().trim())
                .ifsc(request.getIfsc().trim().toUpperCase())
                .bankName(request.getBankName().trim())
                .user(user)
                .build();

        return paymentDetailsRepository.save(paymentDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDetails getUsersPaymentDetails(User user) {
        return paymentDetailsRepository
                .findByUserId(user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment details not found"
                        )
                );
    }
}
