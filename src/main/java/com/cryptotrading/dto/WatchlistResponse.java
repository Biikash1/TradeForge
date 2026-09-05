package com.cryptotrading.dto;

import com.cryptotrading.model.Coin;
import com.cryptotrading.model.Watchlist;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WatchlistResponse {

    private Long id;
    private Long userId;
    private List<Coin> coins; // Return Coin models directly

    public static WatchlistResponse from(Watchlist watchlist) {
        return WatchlistResponse.builder()
                .id(watchlist.getId())
                .userId(watchlist.getUser().getId())
                .coins(watchlist.getCoins()) // Passes the full coin details
                .build();
    }
}