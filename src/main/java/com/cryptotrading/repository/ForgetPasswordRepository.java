package com.cryptotrading.repository;

import com.cryptotrading.model.ForgetPasswordToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForgetPasswordRepository extends JpaRepository<ForgetPasswordToken, String> {

    ForgetPasswordToken findByUserId(Long userId);
}
