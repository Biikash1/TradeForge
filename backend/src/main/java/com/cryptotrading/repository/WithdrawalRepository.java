package com.cryptotrading.repository;

import com.cryptotrading.domain.WithdrawalStatus;
import com.cryptotrading.model.Withdrawal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {
    List<Withdrawal> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Withdrawal> findByStatusOrderByCreatedAtAsc(
            WithdrawalStatus status
    );
}
