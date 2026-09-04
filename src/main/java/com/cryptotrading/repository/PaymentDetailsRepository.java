package com.cryptotrading.repository;

import com.cryptotrading.model.PaymentDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentDetailsRepository extends JpaRepository<PaymentDetails, Long> {

    Optional<PaymentDetails> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}