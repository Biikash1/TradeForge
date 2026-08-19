package com.cryptotrading.repository;

import com.cryptotrading.model.ForgetPasswordToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ForgetPasswordRepository extends JpaRepository<ForgetPasswordToken, String> {

    Optional<ForgetPasswordToken> findByUserId(
            Long userId
    );

    void deleteByUserId(
            Long userId
    );
}
