package com.cryptotrading.model;

import com.cryptotrading.domain.VerificationType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForgetPasswordToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JsonIgnore
    private User user;

    @JsonIgnore
    @Column(
            nullable = false,
            length = 255
    )
    private String otpHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationType verificationType;

    @Column(
            nullable = false,
            length = 255
    )
    private String sendTo;


    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean verified;

    @Column(nullable = false)
    private int attempts;

    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public boolean isExpired() {

        return expiresAt == null ||
                LocalDateTime.now().isAfter(expiresAt);
    }
}
