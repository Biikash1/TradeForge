package com.cryptotrading.service;

import com.cryptotrading.exception.ResourceNotFoundException;
import com.cryptotrading.model.Coin;
import com.cryptotrading.model.User;
import com.cryptotrading.model.Watchlist;
import com.cryptotrading.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchlistServiceImpl implements WatchlistService {

    private final WatchlistRepository watchlistRepository;

    @Override
    @Transactional(readOnly = true)
    public Watchlist findUserWatchlist(Long userId) {
        if (userId == null) throw new IllegalArgumentException("User ID cannot be null");
        return watchlistRepository.findByUserId(userId).orElseThrow(() -> {
            log.warn("Watchlist lookup failed for User {}", userId);
            return new ResourceNotFoundException("Watchlist not found for user: " + userId);
        });
    }

    @Override
    @Transactional
    public Watchlist createWatchlist(User user) {
        if (user == null || user.getId() == null) throw new IllegalArgumentException("Invalid user");
        if (watchlistRepository.existsByUserId(user.getId())) return findUserWatchlist(user.getId());

        Watchlist watchlist = Watchlist.builder().user(user).coins(new ArrayList<>()).build();
        log.info("Created new Watchlist for User {}", user.getId());
        return watchlistRepository.save(watchlist);
    }

    @Override
    @Transactional(readOnly = true)
    public Watchlist findById(Long id) {
        if (id == null) throw new IllegalArgumentException("Watchlist ID cannot be null");
        return watchlistRepository.findById(id).orElseThrow(() -> {
            log.warn("Watchlist lookup failed for ID {}", id);
            return new ResourceNotFoundException("Watchlist not found with id: " + id);
        });
    }

    @Override
    @Transactional
    public Watchlist toggleCoin(Coin coin, User user) {
        if (coin == null || coin.getId() == null || user == null || user.getId() == null) {
            throw new IllegalArgumentException("Invalid coin or user");
        }

        Watchlist watchlist;
        try {
            watchlist = findUserWatchlist(user.getId());
        } catch (ResourceNotFoundException exception) {
            watchlist = createWatchlist(user);
        }

        if (watchlist.getCoins() == null) watchlist.setCoins(new ArrayList<>());

        boolean exists = watchlist.getCoins().stream().anyMatch(c -> c.getId() != null && c.getId().equals(coin.getId()));
        if (exists) {
            watchlist.getCoins().removeIf(c -> c.getId() != null && c.getId().equals(coin.getId()));
            log.info("Removed Coin {} from Watchlist for User {}", coin.getSymbol(), user.getId());
        } else {
            watchlist.getCoins().add(coin);
            log.info("Added Coin {} to Watchlist for User {}", coin.getSymbol(), user.getId());
        }

        return watchlistRepository.save(watchlist);
    }
}