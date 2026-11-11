package com.cryptotrading.service;

import com.cryptotrading.dto.PortfolioAnalyticsResponse;
import com.cryptotrading.model.Asset;
import com.cryptotrading.model.User;
import com.cryptotrading.repository.PortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    private final AssetService assetService;

    @Override
    public PortfolioAnalyticsResponse getPortfolioAnalytics(User user) {
        List<Asset> assets = assetService.getUserAssets(user.getId());

        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal currentValue = BigDecimal.ZERO;

        BigDecimal highestGain = BigDecimal.valueOf(-Double.MAX_VALUE);
        BigDecimal lowerGain = BigDecimal.valueOf(Double.MAX_VALUE);

        PortfolioAnalyticsResponse.AssetPerformance bestPerformer = null;
        PortfolioAnalyticsResponse.AssetPerformance worstPerformer = null;

        for(Asset asset : assets) {
            BigDecimal qty = asset.getQuantity();
            BigDecimal buyPrice = asset.getBuyPrice();
            BigDecimal currentPrice = BigDecimal.valueOf(asset.getCoin().getCurrentPrice());

            BigDecimal investedValue = qty.multiply(buyPrice);
            BigDecimal liveValue  = qty.multiply(currentPrice);

            totalInvested = totalInvested.add(investedValue);
            currentValue = currentValue.add(liveValue);

            // Pre-Asset Performance
            if(buyPrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal gainAmount = liveValue.subtract(investedValue);
                BigDecimal gainPercentage = currentPrice.subtract(buyPrice)
                        .divide(buyPrice, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

                if(gainPercentage.compareTo(highestGain) > 0) {
                    highestGain = gainPercentage;
                    bestPerformer = buildPerformance(asset, gainPercentage, gainAmount);
                }

                if(gainPercentage.compareTo(lowerGain) < 0) {
                    lowerGain = gainPercentage;
                    worstPerformer = buildPerformance(asset, gainPercentage, gainAmount);
                }
            }
        }

        BigDecimal netPnL = currentValue.subtract(totalInvested);
        BigDecimal netPnLPercentage = BigDecimal.ZERO;

        if(totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            netPnLPercentage = netPnL.divide(totalInvested, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return PortfolioAnalyticsResponse.builder()
                .totalInvested(totalInvested.setScale(2, RoundingMode.HALF_UP))
                .currentValue(currentValue.setScale(2, RoundingMode.HALF_UP))
                .netPnL(netPnL.setScale(2, RoundingMode.HALF_UP))
                .netPnLPercentage(netPnLPercentage.setScale(2, RoundingMode.HALF_UP))
                .bestPerformer(bestPerformer)
                .worstPerformer(worstPerformer)
                .build();
    }

    private PortfolioAnalyticsResponse.AssetPerformance buildPerformance(
            Asset asset, BigDecimal gainPct, BigDecimal gainAmt) {

        return PortfolioAnalyticsResponse.AssetPerformance.builder()
                .coinId(asset.getCoin().getId())
                .symbol(asset.getCoin().getSymbol())
                .image(asset.getCoin().getImage())
                .gainPercentage(gainPct.setScale(2, RoundingMode.HALF_UP))
                .gainAmount(gainAmt.setScale(2, RoundingMode.HALF_UP))
                .build();
    }
}
