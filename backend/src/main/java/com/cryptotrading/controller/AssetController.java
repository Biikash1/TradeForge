package com.cryptotrading.controller;

import com.cryptotrading.model.Asset;
import com.cryptotrading.model.User;
import com.cryptotrading.service.AssetService;
import com.cryptotrading.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    private final UserService userService;

    @GetMapping("/{assetId}")
    public ResponseEntity<Asset> getAssetById(
            @PathVariable Long assetId,
            @RequestHeader("Authorization") String jwt){

        User user = userService.findUserProfileByJwt(jwt);

        Asset asset = assetService.getAssetById(assetId);

        if (!asset.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have access to this asset");
        }

        return ResponseEntity.ok(asset);
    }

    @GetMapping("/coin/{coinId}/user")
    public ResponseEntity<Asset> getAssetByUserIdAndCoinId(
            @PathVariable String coinId,
            @RequestHeader("Authorization") String jwt){

        User user = userService.findUserProfileByJwt(jwt);
        Asset asset = assetService.findAssetByUserIdAndCoinId(user.getId(), coinId);
        return ResponseEntity.ok().body(asset);

    }

    @GetMapping
    public ResponseEntity<List<Asset>> getAssetsForUser(
            @RequestHeader("Authorization") String jwt){

        User user = userService.findUserProfileByJwt(jwt);
        List<Asset> assets = assetService.getUserAssets(user.getId());
        return ResponseEntity.ok().body(assets);
    }
}
