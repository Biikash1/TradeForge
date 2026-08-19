package com.cryptotrading.service;

import com.cryptotrading.exception.ResourceNotFoundException;
import com.cryptotrading.model.Coin;
import com.cryptotrading.model.User;
import com.cryptotrading.model.Watchlist;
import com.cryptotrading.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WatchlistServiceImpl implements WatchlistService{

    private final WatchlistRepository watchlistRepository;

    @Override
    @Transactional(readOnly = true)
    public Watchlist findUserWatchlist(Long userId){
        if (userId == null) {
            throw new IllegalArgumentException(
                    "User ID cannot be null"
            );
        }

        return watchlistRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Watchlist not found for user: " + userId
                        )
                );
    }

    @Override
    @Transactional
    public Watchlist createWatchlist(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException(
                    "Invalid user"
            );
        }

        if (watchlistRepository.existsByUserId(user.getId())) {
            return findUserWatchlist(user.getId());
        }

        Watchlist watchlist = Watchlist.builder()
                .user(user)
                .build();

        return watchlistRepository.save(watchlist);
    }

    @Override
    @Transactional(readOnly = true)
    public Watchlist findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "Watchlist ID cannot be null"
            );
        }

        return watchlistRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Watchlist not found with id: " + id
                        )
                );
    }

    @Override
    @Transactional
    public Watchlist toggleCoin(Coin coin, User user){
        if (coin == null || coin.getId() == null) {
            throw new IllegalArgumentException(
                    "Invalid coin"
            );
        }

        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException(
                    "Invalid user"
            );
        }

        Watchlist watchlist;

        try {
            watchlist = findUserWatchlist(user.getId());
        } catch (ResourceNotFoundException exception) {
            watchlist = createWatchlist(user);
        }

        if (watchlist.getCoins().contains(coin)) {
            watchlist.getCoins().remove(coin);
        } else {
            watchlist.getCoins().add(coin);
        }

        return watchlistRepository.save(watchlist);
    }
}
