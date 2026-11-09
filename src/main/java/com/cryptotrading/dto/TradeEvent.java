package com.cryptotrading.dto;

import com.cryptotrading.domain.OrderType;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeEvent(
       String eventId,
       Long orderId,
       Long userId,
       String coinId,
       OrderType orderType,
       BigDecimal quantity,
       BigDecimal price,
       Instant timestamp
) {}
