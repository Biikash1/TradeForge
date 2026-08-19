package com.cryptotrading.repository;

import com.cryptotrading.model.TwoFactorOTP;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TwoFactorOtpRepository extends JpaRepository<TwoFactorOTP, String> {

    Optional<TwoFactorOTP> findByUserId(Long UserId);

    void deleteByUserId(Long userId);
}
