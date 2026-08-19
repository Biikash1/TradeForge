package com.cryptotrading.repository;

import com.cryptotrading.domain.VerificationPurpose;
import com.cryptotrading.domain.VerificationType;
import com.cryptotrading.model.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findTopByUserIdAndVerificationTypeAndVerificationPurposeOrderByCreatedAtDesc(
            Long userId,
            VerificationType verificationType,
            VerificationPurpose verificationPurpose
    );

    void deleteByUserIdAndVerificationTypeAndVerificationPurpose(
            Long userId,
            VerificationType verificationType,
            VerificationPurpose verificationPurpose
    );
}
