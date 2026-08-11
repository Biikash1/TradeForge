package com.cryptotrading.service;

import com.cryptotrading.exception.CoinApiException;
import com.cryptotrading.model.Coin;
import com.cryptotrading.repository.CoinRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.util.UriComponentsBuilder;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;


@Service
@RequiredArgsConstructor
public class CoinServiceImpl implements CoinService{

    private final CoinRepository coinRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${coingecko.base-url}")
    private String baseUrl;

    @Override
    public List<Coin> getCoinList(int page) {

        if (page < 1) {
            throw new IllegalArgumentException(
                    "Page must be greater than 0"
            );
        }

        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/coins/markets")
                .queryParam("vs_currency", "usd")
                .queryParam("per_page", 10)
                .queryParam("page", page)
                .toUriString();

        return get(
                url,
                new TypeReference<List<Coin>>() {}
        );
    }

    @Override
    public JsonNode getMarketChart(String coinId, int days)  {

        if (coinId == null || coinId.isBlank()) {
            throw new IllegalArgumentException(
                    "Coin ID cannot be empty"
            );
        }

        if (days <= 0) {
            throw new IllegalArgumentException(
                    "Days must be greater than 0"
            );
        }

        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/coins/{coinId}/market_chart")
                .queryParam("vs_currency", "usd")
                .queryParam("days", days)
                .buildAndExpand(coinId)
                .toUriString();

        return getJson(url);
    }

    @Override
    public JsonNode getCoinDetails(String coinId) {

        if (coinId == null || coinId.isBlank()) {
            throw new IllegalArgumentException(
                    "Coin ID cannot be empty"
            );
        }

        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/coins/{coinId}")
                .buildAndExpand(coinId)
                .toUriString();

        JsonNode response = getJson(url);

        Coin coin = mapCoinDetails(response);

        coinRepository.save(coin);

        return response;
    }

    @Override
    public Coin findById(String coinId) {
        return coinRepository.findById(coinId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Coin not found: " + coinId
                        )
                );
    }

    @Override
    public JsonNode searchCoin(String keyword) {

        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException(
                    "Search keyword cannot be empty"
            );
        }

        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/search")
                .queryParam("query", keyword)
                .toUriString();

        return getJson(url);
    }

    @Override
    public JsonNode getTop50CoinsByMarketCapRank()  {

        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/coins/markets")
                .queryParam("vs_currency", "usd")
                .queryParam("per_page", 50)
                .queryParam("page", 1)
                .toUriString();

        return getJson(url);
    }

    @Override
    public JsonNode getTrendingCoins() {

        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/search/trending")
                .toUriString();

        return getJson(url);
    }

    private JsonNode getJson(String url) {


        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            createHttpEntity(),
                            String.class
                    );
            return objectMapper.readTree(
                    response.getBody()
            );

        } catch (HttpClientErrorException e) {
            throw new CoinApiException(
                    "CoinGecko request failed: " +
                    e.getStatusCode()
            );
        } catch (HttpServerErrorException e) {

            throw new CoinApiException(
                    "CoinGecko server is currently unavailable"
            );

        } catch (Exception e) {

            throw new CoinApiException(
                    "Failed to process CoinGecko response",
                    e
            );
        }
    }

    private <T> T get(
            String url,
            TypeReference<T> typeReference
    ) {

        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            createHttpEntity(),
                            String.class
                    );
            return objectMapper.readValue(
                    response.getBody(),
                    typeReference
            );
        } catch (HttpClientErrorException e) {

            throw new CoinApiException(
                    "CoinGecko request failed: " +
                            e.getStatusCode()
            );

        } catch (HttpServerErrorException e) {

            throw new CoinApiException(
                    "CoinGecko server is currently unavailable"
            );

        } catch (Exception e) {

            throw new CoinApiException(
                    "Failed to process CoinGecko response",
                    e
            );
        }
    }

    private HttpEntity<Void> createHttpEntity() {

        HttpHeaders headers = new HttpHeaders();

        headers.set(
                HttpHeaders.ACCEPT,
                "application/json"
        );

        return new HttpEntity<>(headers);
    }

    private Coin mapCoinDetails(JsonNode json) {

        JsonNode marketData = json.path("market_data");

        Coin coin = new Coin();

        coin.setId(
                json.path("id").asText(null)
        );

        coin.setName(
                json.path("name").asText(null)
        );

        coin.setSymbol(
                json.path("symbol").asText(null)
        );

        coin.setImage(
                json.path("image")
                        .path("large")
                        .asText(null)
        );

        coin.setMarketCapRank(
                json.path("market_cap_rank").isNumber()
                        ? json.path("market_cap_rank").asLong()
                        : null
        );

        coin.setCurrentPrice(
                getUsdDecimal(
                        marketData,
                        "current_price"
                )
        );

        coin.setMarketCap(
                getUsdDecimal(
                        marketData,
                        "market_cap"
                )
        );

        coin.setFullyDilutedValuation(
                getUsdDecimal(
                        marketData,
                        "fully_diluted_valuation"
                )
        );

        coin.setTotalVolume(
                getUsdDecimal(
                        marketData,
                        "total_volume"
                )
        );

        coin.setHigh24h(
                getUsdDecimal(
                        marketData,
                        "high_24h"
                )
        );

        coin.setLow24h(
                getUsdDecimal(
                        marketData,
                        "low_24h"
                )
        );

        coin.setPriceChange24h(
                getUsdDecimal(
                        marketData,
                        "price_change_24h"
                )
        );

        coin.setPriceChangePercentage24h(
                getDecimal(
                        marketData,
                        "price_change_percentage_24h"
                )
        );

        coin.setMarketCapChange24h(
                getUsdDecimal(
                        marketData,
                        "market_cap_change_24h"
                )
        );

        coin.setMarketCapChangePercentage24h(
                getDecimal(
                        marketData,
                        "market_cap_change_percentage_24h"
                )
        );

        coin.setCirculatingSupply(
                getDecimal(
                        marketData,
                        "circulating_supply"
                )
        );

        coin.setTotalSupply(
                getDecimal(
                        marketData,
                        "total_supply"
                )
        );

        coin.setMaxSupply(
                getDecimal(
                        marketData,
                        "max_supply"
                )
        );

        coin.setAth(
                getUsdDecimal(
                        marketData,
                        "ath"
                )
        );

        coin.setAthChangePercentage(
                getDecimal(
                        marketData,
                        "ath_change_percentage"
                )
        );

        coin.setAthDate(
                parseInstant(
                        marketData.path("ath_date")
                                .path("usd")
                                .asText(null)
                )
        );

        coin.setAtl(
                getUsdDecimal(
                        marketData,
                        "atl"
                )
        );

        coin.setAtlChangePercentage(
                getDecimal(marketData,
                        "atl_change_percentage"
                )
        );

        coin.setAtlDate(
                parseInstant(
                        marketData.path("atl_date")
                                .path("usd")
                                .asText(null)
                )
        );

        coin.setLastUpdated(
                parseInstant(
                        json.path("last_updated")
                                .asText(null)
                )
        );

        return coin;
    }

    private BigDecimal getUsdDecimal(
            JsonNode parent,
            String field
    ) {
        JsonNode value = parent
                .path(field)
                .path("usd");

        return value.isNumber()
                ? value.decimalValue()
                : null;
    }

    private BigDecimal getDecimal(
            JsonNode parent,
            String field
    ) {
        JsonNode value = parent.path(field);

        return value.isNumber()
                ? value.decimalValue()
                : null;
    }

    private Instant parseInstant(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(value);
        } catch (Exception e) {
            return null;
        }
    }
}
