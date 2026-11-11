package com.cryptotrading.controller;

import com.cryptotrading.dto.PortfolioAnalyticsResponse;
import com.cryptotrading.model.User;
import com.cryptotrading.repository.PortfolioService;
import com.cryptotrading.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final UserService userService;

    public ResponseEntity<PortfolioAnalyticsResponse> getPortfolioAnalytics(
            @RequestHeader("Authorization") String jwt) {

        User user = userService.findUserProfileByJwt(jwt);
        PortfolioAnalyticsResponse analytics = portfolioService.getPortfolioAnalytics(user);

        return ResponseEntity.ok(analytics);
    }
}
