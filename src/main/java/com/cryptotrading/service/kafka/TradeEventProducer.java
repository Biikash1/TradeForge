package com.cryptotrading.service.kafka;

import com.cryptotrading.dto.TradeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeEventProducer {

    private static final String TOPIC = "trade-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendTradeEvent(TradeEvent event) {
        kafkaTemplate.send(TOPIC, event.eventId(), event);
        log.info("Produced TradeEvent for Order ID: {}", event.orderId());
    }
}