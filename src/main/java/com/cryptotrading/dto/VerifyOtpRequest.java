package com.cryptotrading.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank(message = "OTP is required")
    private String otp;

    @NotBlank(message = "Session is required")
    private String session;
}
