package com.cryptotrading.dto;

import com.cryptotrading.model.Wallet;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class WalletResponse {

    private Long id;
    private BigDecimal balance;

    public static WalletResponse from(Wallet wallet) {

        return WalletResponse.builder()
                .id(wallet.getId())
                .balance(wallet.getBalance())
                .build();
    }
}
