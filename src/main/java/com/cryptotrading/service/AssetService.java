package com.cryptotrading.service;

import com.cryptotrading.model.Asset;
import com.cryptotrading.model.Coin;
import com.cryptotrading.model.User;

import java.util.List;

public interface AssetService {

    Asset createAsset(User user, Coin coin, double quantity);

    Asset getAssetById(Long assetId) throws Exception;

    Asset getAssetByUserIdAndId(Long userid, Long assetId) throws Exception;

    List<Asset> getUserAssets(Long userId);

    Asset updateAsset(Long assetId, double quantity) throws Exception;

    Asset findAssetByUserIdAndCoinId(Long userId, String coinId);

    void deleteAsset(Long assetId);
}
