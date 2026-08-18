package com.cryptotrading.service;

import com.cryptotrading.model.TwoFactorOTP;
import com.cryptotrading.model.User;

public interface TwoFactorOtpService {

    TwoFactorOTP createTwoFactorOtp(User user, String otp);

    TwoFactorOTP findByUser(Long userId);

    TwoFactorOTP findById(String id);

    boolean verifyTwoFactorOtp(TwoFactorOTP twoFactorOTP, String otp);

    void deleteTwoFactorOtp(TwoFactorOTP twoFactorOTP);
}
