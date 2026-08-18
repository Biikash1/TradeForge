package com.cryptotrading.controller;

import com.cryptotrading.domain.OrderType;
import com.cryptotrading.dto.OrderRequest;
import com.cryptotrading.dto.OrderResponse;
import com.cryptotrading.model.Coin;
import com.cryptotrading.model.Order;
import com.cryptotrading.model.User;
import com.cryptotrading.service.CoinService;
import com.cryptotrading.service.OrderService;
import com.cryptotrading.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;
    private final CoinService coinService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("Authorization") String jwt,
            @Valid @RequestBody OrderRequest request) {

        User user = userService.findUserProfileByJwt(jwt);
        Coin coin = coinService.findById(request.getCoinId());

        Order order = orderService.processOrder(coin, request.getQuantity(), request.getOrderType(), user);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(OrderResponse.from(order));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long orderId)  {

        User user = userService.findUserProfileByJwt(jwt);

        Order order = orderService.getOrderById(orderId);
        if (!order.getUser()
                .getId()
                .equals(user.getId())) {

            throw new RuntimeException(
                    "You don't have access to this order"
            );
        }

        return ResponseEntity.ok( OrderResponse.from(order));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrderForUser(
            @RequestHeader("Authorization") String jwt,
            @RequestParam(required = false) OrderType orderType,
            @RequestParam(required = false) String assetSymbol){

       User user = userService.findUserProfileByJwt(jwt);

        List<Order> orders =
                orderService.getAllOrderOfUser(
                        user.getId(),
                        orderType,
                        assetSymbol
                );

        List<OrderResponse> response = orders.stream()
                .map(OrderResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

}
