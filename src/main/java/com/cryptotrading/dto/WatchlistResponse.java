package com.cryptotrading.dto;

import com.cryptotrading.model.Watchlist;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WatchlistResponse {

    private Long id;
    private Long userId;
    private List<String> coinIds;

    public static WatchlistResponse from(
            Watchlist watchlist
    ) {

        return WatchlistResponse.builder()
                .id(watchlist.getId())
                .userId(watchlist.getUser().getId())
                .coinIds(
                        watchlist.getCoins()
                                .stream()
                                .map(coin -> coin.getId())
                                .toList()
                )
                .build();
    }
}
