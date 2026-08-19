package com.cryptotrading.repository;

import com.cryptotrading.model.PaymentDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentDetailsRepository extends JpaRepository<PaymentDetails, Long> {

    Optional<PaymentDetails> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
