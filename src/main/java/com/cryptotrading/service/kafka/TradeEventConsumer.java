package com.cryptotrading.service.kafka;

import com.cryptotrading.dto.TradeEvent;
import com.cryptotrading.model.ProcessedEvent;
import com.cryptotrading.model.User;
import com.cryptotrading.repository.ProcessedEventRepository;
import com.cryptotrading.repository.UserRepository;
import com.cryptotrading.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeEventConsumer {

    private final ProcessedEventRepository processedEventRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @KafkaListener(topics = "trade-events", groupId = "tradeforge-async-group")
    @Transactional
    public void handleTradeEvent(TradeEvent event) {
        log.info("Received TradeEvent: {}", event.eventId());

        // 1. Idempotency Check (The "walletCredited" idea applied to events)
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("Event {} already processed. Skipping.", event.eventId());
            return;
        }

        try {
            // 2. Process Asynchronous Work
            processNotifications(event);

            // 3. Mark as Processed
            processedEventRepository.save(new ProcessedEvent(event.eventId(), Instant.now()));
            log.info("Successfully processed TradeEvent: {}", event.eventId());

        } catch (Exception e) {
            log.error("Failed to process TradeEvent: {}", event.eventId(), e);
            throw e; // Throwing will cause Kafka to retry based on your retry policy
        }
    }

    private void processNotifications(TradeEvent event) {
        log.info("Sending notifications for user {}", event.userId());

        //  Fetch the user to get their email address
        Optional<User> userOptional = userRepository.findById(event.userId());

        if(userOptional.isPresent()) {
            User user = userOptional.get();
            // Send the automated email
            emailService.sendTradeSuccessEmail(user.getEmail(), event);
            log.info("Trade success email sent to {}", user.getEmail());
        } else {
            log.warn("Cannot send email. User not found for Id: {}", event.userId());
        }
    }
}
