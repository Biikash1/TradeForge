package com.cryptotrading.dto;

import com.cryptotrading.model.VerificationCode;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GeneratedOtp {

    private String otp;
    private VerificationCode verificationCode;
}