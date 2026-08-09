package com.cryptotrading.service;

import com.cryptotrading.model.Asset;
import com.cryptotrading.model.Coin;
import com.cryptotrading.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService{



    @Override
    public Asset createAsset(User user, Coin coin, double quantity) {
        return null;
    }

    @Override
    public Asset getAssetById(Long assetId) {
        return null;
    }

    @Override
    public Asset getAssetByUserIdAndId(Long userid, Long assetId) {
        return null;
    }

    @Override
    public List<Asset> getUserAssets(Long userId) {
        return List.of();
    }

    @Override
    public Asset updateAsset(Long assetId, double quantity) {
        return null;
    }

    @Override
    public Asset findAssetByUserIdAndCoinId(Long userId, String coinId) {
        return null;
    }

    @Override
    public void deleteAsset(Long assetId) {

    }
}
