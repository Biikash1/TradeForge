package com.cryptotrading.model;

import com.cryptotrading.domain.USER_ROLE;
import com.cryptotrading.dto.TwoFactorAuth;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    private Long mobileNumber;

    @Embedded
    @Builder.Default
    private TwoFactorAuth twoFactorAuth = new TwoFactorAuth();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private USER_ROLE role = USER_ROLE.CUSTOMER;

}
