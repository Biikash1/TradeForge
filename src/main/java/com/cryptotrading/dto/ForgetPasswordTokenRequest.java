package com.cryptotrading.dto;

import com.cryptotrading.domain.VerificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ForgetPasswordTokenRequest {

    @NotBlank(message = "Send to cannot be empty")
    private String sendTo;

    @NotNull(message = "Verification type is required")
    private VerificationType verificationType;
}
