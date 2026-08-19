package com.cryptotrading.dto;

import com.cryptotrading.domain.USER_ROLE;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;

    private String fullName;

    private String email;

    private String mobile;

    private USER_ROLE role;

    private boolean twoFactorEnabled;
}
