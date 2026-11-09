package com.cryptotrading.service;

import com.cryptotrading.exception.ResourceNotFoundException;
import com.cryptotrading.model.Asset;
import com.cryptotrading.model.Coin;
import com.cryptotrading.model.User;
import com.cryptotrading.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;

    @Override
    @Transactional
    public Asset createAsset(User user, Coin coin, BigDecimal quantity) {
        if (user == null) {
            log.warn("createAsset failed: User is null");
            throw new IllegalArgumentException("User cannot be null");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("createAsset failed for User {}: Quantity must be positive. Provided: {}", user.getId(), quantity);
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        if (coin == null || coin.getCurrentPrice() <= 0) {
            log.warn("createAsset failed for User {}: Invalid coin or price.", user.getId());
            throw new IllegalArgumentException("Coin current price is not available");
        }

        Asset existingAsset = assetRepository.findByUserIdAndCoinId(user.getId(), coin.getId()).orElse(null);

        if (existingAsset != null) {
            existingAsset.setQuantity(existingAsset.getQuantity().add(quantity));
            log.info("Updated existing asset {} for User {}. New quantity: {}", coin.getSymbol(), user.getId(), existingAsset.getQuantity());
            return assetRepository.save(existingAsset);
        }

        Asset asset = new Asset();
        asset.setUser(user);
        asset.setCoin(coin);
        asset.setQuantity(quantity);
        asset.setBuyPrice(BigDecimal.valueOf(coin.getCurrentPrice()));

        log.info("Created new asset {} for User {}. Quantity: {}", coin.getSymbol(), user.getId(), quantity);
        return assetRepository.save(asset);
    }

    @Override
    @Transactional(readOnly = true)
    public Asset getAssetById(Long assetId) {
        return assetRepository.findById(assetId).orElseThrow(() -> {
            log.warn("getAssetById failed: Asset not found for id {}", assetId);
            return new ResourceNotFoundException("Asset not found with id: " + assetId);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Asset getAssetByUserIdAndId(Long userId, Long assetId) {
        return assetRepository.findByIdAndUserId(assetId, userId).orElseThrow(() -> {
            log.warn("getAssetByUserIdAndId failed: Asset {} not found for User {}", assetId, userId);
            return new ResourceNotFoundException("Asset not found with id: " + assetId);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Asset> getUserAssets(Long userId) {
        log.debug("Fetching all assets for User {}", userId);
        return assetRepository.findByUserId(userId);
    }

    @Transactional
    @Override
    public Asset updateAsset(Long assetId, BigDecimal quantity) {
        if (quantity == null) {
            log.warn("updateAsset failed for assetId {}: Quantity is null", assetId);
            throw new IllegalArgumentException("Quantity cannot be null");
        }

        Asset asset = getAssetById(assetId);
        BigDecimal updatedQuantity = asset.getQuantity().add(quantity);

        if (updatedQuantity.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("updateAsset failed for assetId {}: Resulting quantity would be negative ({})", assetId, updatedQuantity);
            throw new IllegalArgumentException("Asset quantity cannot be negative");
        }

        asset.setQuantity(updatedQuantity);
        log.info("Updated assetId {}. Quantity changed by {}. New total: {}", assetId, quantity, updatedQuantity);
        return assetRepository.save(asset);
    }

    @Override
    @Transactional(readOnly = true)
    public Asset findAssetByUserIdAndCoinId(Long userId, String coinId) {
        return assetRepository.findByUserIdAndCoinId(userId, coinId).orElse(null);
    }

    @Override
    @Transactional
    public void deleteAsset(Long assetId) {
        if (!assetRepository.existsById(assetId)) {
            log.warn("deleteAsset failed: Asset {} does not exist", assetId);
            throw new ResourceNotFoundException("Asset not found with id: " + assetId);
        }
        log.info("Deleted assetId {}", assetId);
        assetRepository.deleteById(assetId);
    }
}