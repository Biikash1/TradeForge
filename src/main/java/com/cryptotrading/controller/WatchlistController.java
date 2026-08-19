package com.cryptotrading.controller;

import com.cryptotrading.dto.WatchlistResponse;
import com.cryptotrading.model.Coin;
import com.cryptotrading.model.User;
import com.cryptotrading.model.Watchlist;
import com.cryptotrading.service.CoinService;
import com.cryptotrading.service.UserService;
import com.cryptotrading.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;
    private final UserService userService;
    private final CoinService coinService;


    // Add/remove coin from watchlist
    @PostMapping("/coin/{coinId}/toggle")
    public ResponseEntity<WatchlistResponse> toggleCoin(
            @RequestHeader("Authorization") String jwt,
            @PathVariable String coinId)  {

        User user = userService.findUserProfileByJwt(jwt);
        Coin coin = coinService.findById(coinId);
        Watchlist watchlist = watchlistService.toggleCoin(coin, user);
        return ResponseEntity.ok(
                WatchlistResponse.from(watchlist)
        );
    }

    @GetMapping
    public ResponseEntity<WatchlistResponse> getUserWatchlist(
            @RequestHeader("Authorization") String jwt) {

        User user = userService.findUserProfileByJwt(jwt);
        Watchlist watchlist = watchlistService.findUserWatchlist(user.getId());
        return ResponseEntity.ok(
                WatchlistResponse.from(watchlist)
        );
    }


    @GetMapping("/{watchlistId}")
    public ResponseEntity<WatchlistResponse> getWatchlistById(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long watchlistId) {

        User user = userService.findUserProfileByJwt(jwt);
        Watchlist watchlist = watchlistService.findById(watchlistId);

        if (!watchlist.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You are not authorized to access this watchlist");
        }

        return ResponseEntity.ok(
                WatchlistResponse.from(watchlist)
        );
    }

}
