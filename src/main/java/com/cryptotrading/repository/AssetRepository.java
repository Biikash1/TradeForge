package com.cryptotrading.repository;

import com.cryptotrading.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    List<Asset> findByUserId(Long userId);

    Optional<Asset> findByUserIdAndCoinId(
            Long userId,
            String coinId
    );

    Optional<Asset> findByIdAndUserId(
            Long assetId,
            Long userId
    );
}
