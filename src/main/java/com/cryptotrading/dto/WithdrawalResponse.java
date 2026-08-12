package com.cryptotrading.dto;

import com.cryptotrading.domain.WithdrawalStatus;
import com.cryptotrading.model.Withdrawal;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class WithdrawalResponse {

    private Long id;
    private BigDecimal amount;
    private WithdrawalStatus status;
    private Instant createdAt;
    private Instant processedAt;

    public static WithdrawalResponse from(Withdrawal withdrawal) {

        return WithdrawalResponse.builder()
                .id(withdrawal.getId())
                .amount(withdrawal.getAmount())
                .status(withdrawal.getStatus())
                .createdAt(withdrawal.getCreatedAt())
                .processedAt(withdrawal.getProcessedAt())
                .build();
    }
}
