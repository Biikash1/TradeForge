package com.cryptotrading.dto;

import lombok.Data;

@Data
public class AuthResponse {

    private Long id;
    private String fullName;
    private String email;
    private Long mobileNumber;

    private String jwt;
    private boolean status;
    private String message;

    private boolean twoFactorAuthEnabled;
    private String session;
}
