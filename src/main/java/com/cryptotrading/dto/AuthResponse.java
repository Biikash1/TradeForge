package com.cryptotrading.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private Long id;
    private String fullName;
    private String email;
    private String mobile;

    private String jwt;
    private boolean status;
    private String message;

    private boolean twoFactorAuthEnabled;
    private String session;
}
