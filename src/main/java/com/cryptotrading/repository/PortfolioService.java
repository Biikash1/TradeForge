package com.cryptotrading.repository;

import com.cryptotrading.dto.PortfolioAnalyticsResponse;
import com.cryptotrading.model.User;

public interface PortfolioService {
    PortfolioAnalyticsResponse getPortfolioAnalytics(User user);
}
