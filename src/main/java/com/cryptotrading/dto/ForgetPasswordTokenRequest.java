package com.cryptotrading.dto;

import com.cryptotrading.domain.VerificationType;
import lombok.Data;

@Data
public class ForgetPasswordTokenRequest {
    private String sendTo;
    private VerificationType verificationType;
}
