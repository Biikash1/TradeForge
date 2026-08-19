package com.cryptotrading.service;

import com.cryptotrading.dto.GeneratedOtp;
import com.cryptotrading.domain.VerificationPurpose;
import com.cryptotrading.domain.VerificationType;
import com.cryptotrading.model.User;
import com.cryptotrading.model.VerificationCode;

public interface VerificationCodeService {

    GeneratedOtp generateVerificationCode(
            User user,
            VerificationType verificationType,
            VerificationPurpose verificationPurpose
    );

    VerificationCode getVerificationCodeById(Long id);

    VerificationCode getLatestVerificationCode(
            Long userId,
            VerificationType verificationType,
            VerificationPurpose verificationPurpose
    );

    boolean verifyCode(
            VerificationCode verificationCode,
            String otp
    );

    void deleteVerificationCode(
            VerificationCode verificationCode
    );
}
