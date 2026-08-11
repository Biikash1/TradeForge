package com.cryptotrading.controller;

import com.cryptotrading.model.Coin;
import com.cryptotrading.service.CoinService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/coins")
@RequiredArgsConstructor
public class CoinController {

    private final CoinService coinService;

    @GetMapping
   public ResponseEntity<List<Coin>> getCoinList(
           @RequestParam(defaultValue = "1") int page) {
       return ResponseEntity.ok(
               coinService.getCoinList(page)
       );
    }

    @GetMapping("/{coinId}/chart")
    public ResponseEntity<JsonNode> getMarketChart(
            @PathVariable String coinId,
            @RequestParam(defaultValue = "7") int days) {

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

    @GetMapping("/trading")
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
