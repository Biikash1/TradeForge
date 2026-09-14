package com.cryptotrading.service;

import com.cryptotrading.dto.ApiResponse;

public interface ChatBotService {

    ApiResponse getCoinDetails(String prompt) throws Exception;

    String simpleChat(String prompt);
}
