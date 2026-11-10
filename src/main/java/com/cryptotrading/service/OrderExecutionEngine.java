package com.cryptotrading.service;

import com.cryptotrading.domain.OrderStatus;
import com.cryptotrading.domain.OrderType;
import com.cryptotrading.model.Coin;
import com.cryptotrading.model.Order;
import com.cryptotrading.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderExecutionEngine {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final CoinService coinService;

    // Run every 20 seconds, aligning perfectly with your existing CoinGecko Redis cache TTL
    @Scheduled(fixedRate = 20000)
    @Transactional(readOnly = true)
    public void evaluatePendingOrders() {
        List<OrderType> advancedTypes = List.of(
                OrderType.LIMIT_BUY,
                OrderType.LIMIT_SELL,
                OrderType.STOP_LOSS_BUY,
                OrderType.STOP_LOSS_SELL
        );

        List<Order> pendingOrders = orderRepository.findByStatusAndOrderTypeIn(
                OrderStatus.PENDING,
                advancedTypes
        );

        if(pendingOrders.isEmpty()) {
            return;
        }

        for(Order order : pendingOrders) {
            try{
                Coin coin = coinService.findById(order.getOrderItem().getCoin().getId());
                BigDecimal currentPrice = BigDecimal.valueOf(coin.getCurrentPrice());
                BigDecimal targetPrice = order.getTargetPrice();

                if(shouldExecute(order.getOrderType(), currentPrice, targetPrice)) {
                    log.info("Condition met for Order ID {}: Target {}, Current {}. Triggeruing execution.",
                            order.getId(), targetPrice, currentPrice);
                    // Pass to OrderService to handle the transaction independently
                    orderService.executePendingAdvanceOrder(order.getId(), coin);
                }
            } catch (Exception e) {
                log.error("Error evaluating pending order ID {}", order.getId(), e);
            }
        }
    }

    private boolean shouldExecute(OrderType type, BigDecimal currentPrice, BigDecimal targetPrice) {
        return switch (type) {
            case LIMIT_BUY -> currentPrice.compareTo(targetPrice) <= 0;     // Buy when price drops to/below target
            case LIMIT_SELL -> currentPrice.compareTo(targetPrice) >= 0;    // Sell when price rises to/above target
            case STOP_LOSS_BUY -> currentPrice.compareTo(targetPrice) >= 0; // Buy when price breaks out above target
            case STOP_LOSS_SELL -> currentPrice.compareTo(targetPrice) <= 0;// Sell when price drops to/below target
            default -> false;
        };
    }

}
