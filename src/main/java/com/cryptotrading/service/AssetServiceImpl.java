package com.cryptotrading.service;

import com.cryptotrading.exception.ResourceNotFoundException;
import com.cryptotrading.model.Asset;
import com.cryptotrading.model.Coin;
import com.cryptotrading.model.User;
import com.cryptotrading.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;

    @Override
    @Transactional
    public Asset createAsset(User user,
                             Coin coin,
                             BigDecimal quantity) {

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        // Fixed: Primitive double cannot be compared to null
        if (coin == null || coin.getCurrentPrice() <= 0) {
            throw new IllegalArgumentException("Coin current price is not available");
        }

        Asset existingAsset = assetRepository
                .findByUserIdAndCoinId(user.getId(), coin.getId())
                .orElse(null);

        if (existingAsset != null) {
            existingAsset.setQuantity(existingAsset.getQuantity().add(quantity));
            return assetRepository.save(existingAsset);
        }

        Asset asset = new Asset();
        asset.setUser(user);
        asset.setCoin(coin);
        asset.setQuantity(quantity);
        // Fixed: Convert primitive double to BigDecimal for Asset.buyPrice
        asset.setBuyPrice(BigDecimal.valueOf(coin.getCurrentPrice()));

        return assetRepository.save(asset);
    }

    @Override
    @Transactional(readOnly = true)
    public Asset getAssetById(Long assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Asset not found with id: " + assetId
                        )
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Asset getAssetByUserIdAndId(Long userId, Long assetId) {
        return assetRepository
                .findByIdAndUserId(assetId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Asset not found with id: " + assetId
                        )
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Asset> getUserAssets(Long userId) {
        return assetRepository.findByUserId(userId);
    }

    @Transactional
    @Override
    public Asset updateAsset(Long assetId, BigDecimal quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity cannot be null");
        }

        Asset asset = getAssetById(assetId);

        BigDecimal updatedQuantity = asset.getQuantity().add(quantity);

        if (updatedQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Asset quantity cannot be negative");
        }

        asset.setQuantity(updatedQuantity);

        return assetRepository.save(asset);
    }

    @Override
    @Transactional(readOnly = true)
    public Asset findAssetByUserIdAndCoinId(Long userId, String coinId) {
        return assetRepository
                .findByUserIdAndCoinId(userId, coinId)
                .orElse(null);
    }

    @Override
    @Transactional
    public void deleteAsset(Long assetId) {
        if (!assetRepository.existsById(assetId)) {
            throw new ResourceNotFoundException(
                    "Asset not found with id: " + assetId
            );
        }

        assetRepository.deleteById(assetId);
    }
}