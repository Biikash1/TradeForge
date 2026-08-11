package com.cryptotrading.service;

import com.cryptotrading.model.Coin;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface CoinService {

    List<Coin> getCoinList(int page);

    JsonNode getMarketChart(String coinId, int day);

    JsonNode getCoinDetails(String coinId);

    Coin findById( String coinId);

    JsonNode searchCoin(String keyword);

    JsonNode getTop50CoinsByMarketCapRank();

    JsonNode getTrendingCoins();
}
