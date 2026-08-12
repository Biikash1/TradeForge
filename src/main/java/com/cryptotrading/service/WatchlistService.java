package com.cryptotrading.service;

import com.cryptotrading.model.Coin;
import com.cryptotrading.model.User;
import com.cryptotrading.model.Watchlist;

public interface WatchlistService {

    Watchlist findUserWatchlist(Long userId);

    Watchlist createWatchlist(User user);

    Watchlist findById(Long id);

    Watchlist toggleCoin(Coin coin, User user);
}
