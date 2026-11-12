//package com.cryptotrading.service;
//
//import com.cryptotrading.domain.OrderStatus;
//import com.cryptotrading.domain.OrderType;
//import com.cryptotrading.model.Coin;
//import com.cryptotrading.model.Order;
//import com.cryptotrading.model.OrderItem;
//import com.cryptotrading.model.User;
//import com.cryptotrading.repository.OrderRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.math.BigDecimal;
//import java.util.List;
//
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//public class OrderExecutionEngineTest {
//
//    @Mock
//    private OrderRepository orderRepository;
//
//    @Mock
//    private OrderService orderService;
//
//    @Mock
//    private CoinService coinService;
//
//    @InjectMocks
//    private OrderExecutionEngine orderExecutionEngine;
//
//    private User testUser;
//    private Coin testCoin;
//
//    @BeforeEach
//    void setUp() {
//        testUser = new User();
//        testUser.setId(1L);
//
//        testCoin = new Coin();
//        testCoin.setId("bitcoin");
//        testCoin.setSymbol("btc");
//    }
//
//    @Test
//    void testEvaluatePendingOrders_LimitBuy_ExecutesWhenPriceDrop() {
//        // Arrange: Market price drops to 60,000. Limit buy target is 62,000.
//        testCoin.setCurrentPrice(60000.0);
//
//        OrderItem item = new OrderItem();
//        item.setCoin(testCoin);
//
//        Order pendinfOrder = Order.builder()
//                .id(100L)
//                .user(testUser)
//                .orderType(OrderType.LIMIT_BUY)
//                .status(OrderStatus.PENDING)
//                .targetPrice(BigDecimal.valueOf(62000.0))
//                .orderItem(item)
//                .build();
//
//        when(orderRepository.findByStatusAndOrderTypeIn(eq(OrderStatus.PENDING), anyList()))
//                .thenReturn(List.of(pendinfOrder));
//        when(coinService.findById("bitcoin")).thenReturn(testCoin);
//
//        // Act
//        orderExecutionEngine.evaluatePendingOrders();
//
//        // Assert: Engine SHOULD trigger execution because 60000 <= 62000
//        verify(orderService, times(1)).executePendingAdvanceOrder(100L, testCoin);
//    }
//}
