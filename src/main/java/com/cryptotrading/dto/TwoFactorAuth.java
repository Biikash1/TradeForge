package com.cryptotrading.dto;

import com.cryptotrading.domain.VerificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorAuth {

    private boolean enabled = false;

    private VerificationType sendTo;
}
