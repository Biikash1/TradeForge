package com.cryptotrading.controller;

import com.cryptotrading.model.Coin;
import com.cryptotrading.service.CoinService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

import java.util.List;

@RestController
@RequestMapping("/api/coins")
@RequiredArgsConstructor
public class CoinController {

    private final CoinService coinService;

    @GetMapping
   public ResponseEntity<List<Coin>> getCoinList(
           @RequestParam(defaultValue = "1")
           @Min(value = 1, message = "Page must be greater than 0")
           int page) {

       return ResponseEntity.ok(
               coinService.getCoinList(page)
       );
    }

    @GetMapping("/{coinId}/chart")
    public ResponseEntity<JsonNode> getMarketChart(
            @PathVariable String coinId,
            @RequestParam(defaultValue = "7")
            @Min(value = 1, message = "Days must be greater than 0")
            int days) {

        return ResponseEntity.ok(
                coinService.getMarketChart(coinId, days)
        );
    }

    @GetMapping("/search")
    public ResponseEntity<JsonNode> searchCoin(
            @RequestParam("q") String keyword) {

        return ResponseEntity.ok(
                coinService.searchCoin(keyword)
        );
    }

    @GetMapping("/top50")
    public ResponseEntity<JsonNode> getTop50CoinByMarketCapRank() {

        return ResponseEntity.ok(
                coinService.getTop50CoinsByMarketCapRank()
        );
    }

    @GetMapping("/trending")
    public ResponseEntity<JsonNode> getTradingCoins()  {

        return ResponseEntity.ok(
                coinService.getTrendingCoins()
        );
    }

    @GetMapping("/details/{coinId}")
    public ResponseEntity<JsonNode> getCoinDetails(
            @PathVariable String coinId)  {

        return ResponseEntity.ok(
                coinService.getCoinDetails(coinId)
        );
    }
}
