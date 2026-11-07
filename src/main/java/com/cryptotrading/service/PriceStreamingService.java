package com.cryptotrading.service;

import com.cryptotrading.model.Coin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceStreamingService {

    private final CoinService coinService;
    private final SimpMessagingTemplate messagingTemplate;

    // Fetch and broadcast every 20 seconds to align with the cache
    @Scheduled(fixedRate = 20000)
    public void streamLivePrice() {
        try{
            // Fetches fresh data because the 20s cache has expired
            List<Coin> topCoins = coinService.getCoinList(1);

            // Broadcast the list of coins to any client subscribe to /topic/public/price
            messagingTemplate.convertAndSend("/topic/public/prices", topCoins);
        } catch (Exception e) {
            log.error("Failed to stream live price via WebSocket: {}", e.getMessage());
        }
    }
}
