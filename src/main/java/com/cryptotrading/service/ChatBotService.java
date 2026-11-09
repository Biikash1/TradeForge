package com.cryptotrading.service;

import com.cryptotrading.domain.OrderType;
import com.cryptotrading.dto.ApiResponse;
import com.cryptotrading.model.User;

import java.math.BigDecimal;

public interface ChatBotService {

    ApiResponse getCoinDetails(String prompt) throws Exception;

    String simpleChat(String prompt);

    String  getPreTradeInsight(String coinId,
                               BigDecimal quantity,
                               OrderType orderType,
                               User user);
}
