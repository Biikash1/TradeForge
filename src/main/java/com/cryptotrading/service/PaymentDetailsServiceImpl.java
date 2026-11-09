package com.cryptotrading.service;

import com.cryptotrading.dto.PaymentDetailsRequest;
import com.cryptotrading.model.PaymentDetails;
import com.cryptotrading.model.User;
import com.cryptotrading.repository.PaymentDetailsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
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

        PaymentDetails paymentDetails = paymentDetailsRepository
                .findByUserIdAndAccountNumber(user.getId(), request.getAccountNumber().trim())
                .orElseGet(() -> PaymentDetails.builder().user(user).build());

        paymentDetails.setAccountNumber(request.getAccountNumber().trim());
        paymentDetails.setAccountHolderName(request.getAccountHolderName().trim());
        paymentDetails.setIfsc(request.getIfsc().trim().toUpperCase());
        paymentDetails.setBankName(request.getBankName().trim());

        PaymentDetails saved = paymentDetailsRepository.save(paymentDetails);
        log.info("Saved PaymentDetails (ID: {}) for User {}", saved.getId(), user.getId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentDetails> getUsersPaymentDetails(User user) {
        if (user == null || user.getId() == null) return List.of();
        return paymentDetailsRepository.findByUserId(user.getId());
    }

    @Override
    @Transactional
    public void deletePaymentDetails(Long id, User user) {
        PaymentDetails details = paymentDetailsRepository.findById(id).orElseThrow(() -> {
            log.warn("deletePaymentDetails failed: Details ID {} not found", id);
            return new IllegalArgumentException("Bank details not found");
        });

        if (!details.getUser().getId().equals(user.getId())) {
            log.warn("deletePaymentDetails failed: Unauthorized action by User {} for Details ID {}", user.getId(), id);
            throw new IllegalStateException("Unauthorized action");
        }

        paymentDetailsRepository.delete(details);
        log.info("Deleted PaymentDetails ID {} for User {}", id, user.getId());
    }
}