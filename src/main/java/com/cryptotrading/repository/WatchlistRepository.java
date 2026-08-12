package com.cryptotrading.repository;

import com.cryptotrading.model.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    Optional<Watchlist> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
