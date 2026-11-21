package com.cryptotrading.service.kafka;

import com.cryptotrading.dto.TradeEvent;
import com.cryptotrading.model.OutboxEvent;
import com.cryptotrading.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRelayService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void relayPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findTop50ByPublishedFalseOrderByCreatedAtAsc();
        if (pending.isEmpty()) {
            return;
        }

        for (OutboxEvent outbox : pending) {
            try {
                TradeEvent tradeEvent = objectMapper.readValue(outbox.getPayloadJson(), TradeEvent.class);

                // Wait for broker acknowledgement before marking published
                kafkaTemplate.send(outbox.getTopic(), outbox.getEventKey(), tradeEvent)
                        .get(5, TimeUnit.SECONDS);

                outbox.setPublished(true);
                outboxEventRepository.save(outbox);
                log.info("Relayed OutboxEvent ID {} (Order ID: {}) to Kafka topic '{}'",
                        outbox.getId(), tradeEvent.orderId(), outbox.getTopic());

            } catch (Exception e) {
                log.error("Failed to relay OutboxEvent ID {} (will retry next cycle): {}",
                        outbox.getId(), e.getMessage());
                // Stop this batch if broker is down; remaining events stay published = false
                break;
            }
        }
    }
}