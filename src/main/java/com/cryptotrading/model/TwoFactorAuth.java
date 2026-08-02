package com.cryptotrading.model;

import com.cryptotrading.domain.verificationType;
import lombok.Data;

@Data
public class TwoFactorAuth {
    private boolean isEnabled = false;
    private verificationType sendTo;
}
