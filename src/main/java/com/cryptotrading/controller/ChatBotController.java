package com.cryptotrading.controller;

import com.cryptotrading.dto.ApiResponse;
import com.cryptotrading.dto.PromptRequest;
import com.cryptotrading.service.ChatBotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatBotController {

    private final ChatBotService chatBotService;

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
}