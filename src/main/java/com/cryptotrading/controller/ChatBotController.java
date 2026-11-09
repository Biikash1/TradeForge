package com.cryptotrading.controller;

import com.cryptotrading.dto.ApiResponse;
import com.cryptotrading.dto.OrderRequest;
import com.cryptotrading.dto.PromptRequest;
import com.cryptotrading.model.User;
import com.cryptotrading.service.ChatBotService;
import com.cryptotrading.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatBotController {

    private final ChatBotService chatBotService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse> getCoinDetails(@Valid @RequestBody PromptRequest request) throws Exception {
        ApiResponse response = chatBotService.getCoinDetails(request.getPrompt());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/simple")
    public ResponseEntity<Map<String, String>> simpleChatHandler(@Valid @RequestBody PromptRequest request) {
        String response = chatBotService.simpleChat(request.getPrompt());
        return ResponseEntity.ok(Map.of("message", response));
    }

    @PostMapping("/trade-insight")
    public ResponseEntity<ApiResponse> getTradeInsight(
            @RequestHeader("Authorization") String jwt,
            @Valid @RequestBody OrderRequest request) {

        User user = userService.findUserProfileByJwt(jwt);

        String insight = chatBotService.getPreTradeInsight(
                request.getCoinId(),
                request.getQuantity(),
                request.getOrderType(),
                user
        );

        return ResponseEntity.ok(new ApiResponse(insight));
    }
}