package com.cryptotrading.dto;

import com.cryptotrading.model.PaymentDetails;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentDetailsResponse {
    private Long id;
    private String maskedAccountNumber;
    private String accountHolderName;
    private String ifsc;
    private String bankName;

    public static PaymentDetailsResponse from(PaymentDetails details) {
        String raw = details.getAccountNumber();
        String masked = (raw != null && raw.length() >= 4)
                ? "****" + raw.substring(raw.length() - 4)
                : "****";

        return PaymentDetailsResponse.builder()
                .id(details.getId())
                .maskedAccountNumber(masked)
                .accountHolderName(details.getAccountHolderName())
                .ifsc(details.getIfsc())
                .bankName(details.getBankName())
                .build();
    }
}