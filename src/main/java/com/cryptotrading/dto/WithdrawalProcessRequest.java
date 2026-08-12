package com.cryptotrading.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WithdrawalProcessRequest {

    @NotNull(message = "Decision is required")
    private Boolean accept;
}
