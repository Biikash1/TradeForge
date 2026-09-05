package com.cryptotrading.service;

import com.cryptotrading.dto.PaymentDetailsRequest;
import com.cryptotrading.model.PaymentDetails;
import com.cryptotrading.model.User;
import com.cryptotrading.repository.PaymentDetailsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

        // Check if this specific account number already exists for this user
        PaymentDetails paymentDetails = paymentDetailsRepository
                .findByUserIdAndAccountNumber(user.getId(), request.getAccountNumber().trim())
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
    public List<PaymentDetails> getUsersPaymentDetails(User user) {
        if (user == null || user.getId() == null) {
            return List.of();
        }
        return paymentDetailsRepository.findByUserId(user.getId());
    }

    @Override
    @Transactional
    public void deletePaymentDetails(Long id, User user) {
        PaymentDetails details = paymentDetailsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bank details not found"));

        if (!details.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Unauthorized action");
        }

        paymentDetailsRepository.delete(details);
    }
}