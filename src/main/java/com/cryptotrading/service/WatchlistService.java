package com.cryptotrading.service;

import com.cryptotrading.model.Coin;
import com.cryptotrading.model.User;
import com.cryptotrading.model.Watchlist;

public interface WatchlistService {

    Watchlist findUserWatchList(Long userId) throws Exception;

    Watchlist createWatchList(User user);

    Watchlist findById(Long id) throws Exception;

    Coin addItemToWatchList(Coin coin, User user) throws Exception;
}
