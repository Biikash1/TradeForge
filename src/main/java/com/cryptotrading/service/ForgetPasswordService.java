package com.cryptotrading.service;

import com.cryptotrading.domain.VerificationType;
import com.cryptotrading.model.ForgetPasswordToken;
import com.cryptotrading.model.User;

public interface ForgetPasswordService {

    ForgetPasswordToken createToken(
            User user,
            String id, String otp,
            VerificationType verificationType,
            String sendTo
    );

    ForgetPasswordToken findById(String id);

    ForgetPasswordToken findByUser(Long userId);

    void deleteToken(ForgetPasswordToken token);
}
