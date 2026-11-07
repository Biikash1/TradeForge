package com.cryptotrading.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    /**
     * A dummy endpoint to satisfy Spring WebSocket's internal requirement
     * for at least one registered @MessageMapping handler during startup.
     */
    @MessageMapping("/ping")
    public void ping() {
        // No action needed. The backend only pushes data via PriceStreamingService.
    }
}