package com.cryptotrading.dto;

import com.cryptotrading.domain.VerificationType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorAuth {

    @Builder.Default
    private boolean enabled = false;

    @Enumerated(EnumType.STRING)
    private VerificationType sendTo;
}
