package com.cryptotrading.repository;

import com.cryptotrading.model.PaymentDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentDetailsRepository extends JpaRepository<PaymentDetails, Long> {
    List<PaymentDetails> findByUserId(Long userId);
    Optional<PaymentDetails> findByUserIdAndAccountNumber(Long userId, String accountNumber);
}