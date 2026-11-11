package com.cryptotrading.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PortfolioAnalyticsResponse {
    private BigDecimal totalInvested;
    private BigDecimal currentValue;
    private BigDecimal netPnL;
    private BigDecimal netPnLPercentage;

    private  AssetPerformance bestPerformer;
    private AssetPerformance worstPerformer;

    @Data
    @Builder
    public static class AssetPerformance {
        private String coinId;
        private String symbol;
        private String image;
        private BigDecimal gainPercentage;
        private BigDecimal gainAmount;
    }
}

