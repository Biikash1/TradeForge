package com.cryptotrading.service;

import com.cryptotrading.domain.VerificationType;
import com.cryptotrading.model.User;
import com.cryptotrading.model.VerificationCode;

public interface VerificationCodeService {

     VerificationCode sendVerificationCodeByUser(User user, VerificationType verificationType);

     VerificationCode getVerificationCodeById(Long id) throws Exception;

     VerificationCode getVerificationCodeByUser(Long userId);

     void deleteVerificationCodeById(VerificationCode verificationCode);
}
