package com.cryptotrading.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetailsRequest {

    @NotBlank(message = "Account number is required")
    @Size(min = 9, max = 18, message = "Invalid account number")
    private String accountNumber;

    @NotBlank(message = "Account holder name is required")
    @Size(max = 100, message = "Account holder name is too long")
    private String accountHolderName;

    @NotBlank(message = "IFSC is required")
    @Pattern(
            regexp = "^[A-Z]{4}0[A-Z0-9]{6}$",
            message = "Invalid IFSC code"
    )
    private String ifsc;

    @NotBlank(message = "Bank name is required")
    @Size(max = 100, message = "Bank name is too long")
    private String bankName;
}
