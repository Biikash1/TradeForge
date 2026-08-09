package com.cryptotrading.controller;

import com.cryptotrading.domain.OrderType;
import com.cryptotrading.dto.OrderRequest;
import com.cryptotrading.model.Coin;
import com.cryptotrading.model.Order;
import com.cryptotrading.model.User;
import com.cryptotrading.service.CoinService;
import com.cryptotrading.service.OrderService;
import com.cryptotrading.service.UserService;
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
   // private final WalletTransactionService walletTransactionService;

    @PostMapping("/pay")
    public ResponseEntity<Order> payOrderPayment(
            @RequestHeader("Authorization") String jwt,
            @RequestBody OrderRequest request) throws Exception {

        User user = userService.findUserProfileByJwt(jwt);
        Coin coin = coinService.findById(request.getCoinId());

        Order order = orderService.processOrder(coin, request.getQuantity(), request.getOrderType(), user);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long orderId) throws Exception {

        User user = userService.findUserProfileByJwt(jwt);

        Order order = orderService.getOrderById(orderId);
        if(order.getUser().equals(user.getId())) {
            return ResponseEntity.ok(order);
        }else {
           throw new Exception("You don't have access");
        }
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrdersRorUsers(
            @RequestHeader("Authorization") String jwt,
            @RequestParam(required = false) OrderType orderType,
            @RequestParam(required = false) String asset_symbol) throws Exception {

       Long userId = userService.findUserProfileByJwt(jwt).getId();

       List<Order> userOrders = orderService.getAllOrderOfUser(userId, orderType, asset_symbol);
       return ResponseEntity.ok(userOrders);
    }

}
