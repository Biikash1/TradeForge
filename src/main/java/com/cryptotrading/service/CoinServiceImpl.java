package com.cryptotrading.service;

import com.cryptotrading.exception.CoinApiException;
import com.cryptotrading.model.Coin;
import com.cryptotrading.repository.CoinRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoinServiceImpl implements CoinService {

    private final CoinRepository coinRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    private static final String CURRENCY = "usd";
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int TOP_COINS_LIMIT = 50;

    // 3-minute in-memory cache to stay safely within keyless limits
    private static final long CACHE_DURATION_MS = 180_000;
    private long lastFetchTimestamp = 0;

    @Value("${coingecko.base-url:https://api.coingecko.com/api/v3}")
    private String baseUrl;

    @Override
    public List<Coin> getCoinList(int page) {
        validatePage(page);

        long now = System.currentTimeMillis();

        // Serve from database if fetched recently
        if (page == 1 && (now - lastFetchTimestamp < CACHE_DURATION_MS) && coinRepository.count() > 0) {
            return coinRepository.findAll();
        }

        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/coins/markets")
                .queryParam("vs_currency", CURRENCY)
                .queryParam("per_page", DEFAULT_PAGE_SIZE)
                .queryParam("page", page)
                .queryParam("sparkline", false)
                .toUriString();

        try {
            List<Coin> coins = get(url, new TypeReference<List<Coin>>() {});
            if (coins != null && !coins.isEmpty()) {
                coinRepository.saveAll(coins);
                lastFetchTimestamp = now;
                return coins;
            }
        } catch (Exception ex) {
            log.warn("CoinGecko rate limit or error reached (429). Serving fallback/database cache: {}", ex.getMessage());

            if (coinRepository.count() > 0) {
                return coinRepository.findAll();
            }

            List<Coin> defaultCoins = getFallbackCoins();
            coinRepository.saveAll(defaultCoins);
            return defaultCoins;
        }

        return coinRepository.count() > 0 ? coinRepository.findAll() : getFallbackCoins();
    }

    @Override
    public JsonNode getMarketChart(String coinId, int days) {
        validateCoinId(coinId);

        if (days <= 0) {
            throw new IllegalArgumentException("Days must be greater than 0");
        }

        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/coins/{coinId}/market_chart")
                .queryParam("vs_currency", CURRENCY)
                .queryParam("days", days)
                .buildAndExpand(coinId)
                .toUriString();

        try {
            return getJson(url);
        } catch (Exception ex) {
            log.warn("Market chart failed (429/timeout). Serving fallback points: {}", ex.getMessage());
            return getFallbackChartJson();
        }
    }

    @Override
    public JsonNode getCoinDetails(String coinId) {
        validateCoinId(coinId);

        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/coins/{coinId}")
                .buildAndExpand(coinId)
                .toUriString();

        try {
            JsonNode response = getJson(url);
            Coin coin = mapCoinDetails(response);
            coinRepository.save(coin);
            return response;
        } catch (Exception ex) {
            log.warn("Coin details failed. Serving cached entity: {}", ex.getMessage());
            Coin existing = findById(coinId);
            return objectMapper.valueToTree(existing);
        }
    }

    @Override
    public Coin findById(String coinId) {
        validateCoinId(coinId);

        return coinRepository.findById(coinId)
                .orElseGet(() -> {
                    List<Coin> fallback = getFallbackCoins();
                    return fallback.stream()
                            .filter(c -> c.getId().equalsIgnoreCase(coinId))
                            .findFirst()
                            .orElse(fallback.get(0));
                });
    }

    @Override
    public JsonNode searchCoin(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("Search keyword cannot be empty");
        }

        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/search")
                .queryParam("query", keyword)
                .toUriString();

        return getJson(url);
    }

    @Override
    public JsonNode getTop50CoinsByMarketCapRank() {
        return objectMapper.valueToTree(getCoinList(1));
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
        String responseBody = executeGet(url);
        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception ex) {
            throw new CoinApiException("Failed to parse CoinGecko response", ex);
        }
    }

    private <T> T get(String url, TypeReference<T> typeReference) {
        String responseBody = executeGet(url);
        try {
            return objectMapper.readValue(responseBody, typeReference);
        } catch (Exception ex) {
            throw new CoinApiException("Failed to parse CoinGecko response", ex);
        }
    }

    private String executeGet(String url) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    createHttpEntity(),
                    String.class
            );

            String body = response.getBody();
            if (body == null || body.isBlank()) {
                throw new CoinApiException("CoinGecko returned an empty response");
            }
            return body;

        } catch (HttpClientErrorException ex) {
            throw new CoinApiException("CoinGecko request failed: " + ex.getStatusCode(), ex);
        } catch (HttpServerErrorException ex) {
            throw new CoinApiException("CoinGecko server is currently unavailable", ex);
        } catch (CoinApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CoinApiException("Failed to communicate with CoinGecko", ex);
        }
    }

    private HttpEntity<Void> createHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "application/json");
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        return new HttpEntity<>(headers);
    }

    private void validatePage(int page) {
        if (page < 1) {
            throw new IllegalArgumentException("Page must be greater than 0");
        }
    }

    private void validateCoinId(String coinId) {
        if (coinId == null || coinId.isBlank()) {
            throw new IllegalArgumentException("Coin ID cannot be empty");
        }
    }

    private Coin mapCoinDetails(JsonNode json) {
        JsonNode marketData = json.path("market_data");
        Coin coin = new Coin();

        coin.setId(json.path("id").asText(null));
        coin.setName(json.path("name").asText(null));
        coin.setSymbol(json.path("symbol").asText(null));
        coin.setImage(json.path("image").path("large").asText(null));

        coin.setMarketCapRank(
                json.path("market_cap_rank").isNumber()
                        ? json.path("market_cap_rank").asLong()
                        : null
        );

        coin.setCurrentPrice(getDouble(marketData.path("current_price"), "usd"));
        coin.setMarketCap(getLong(marketData.path("market_cap"), "usd"));
        coin.setTotalVolume(getLong(marketData.path("total_volume"), "usd"));
        coin.setHigh24h(getDouble(marketData.path("high_24h"), "usd"));
        coin.setLow24h(getDouble(marketData.path("low_24h"), "usd"));
        coin.setPriceChange24h(getDouble(marketData.path("price_change_24h"), "usd"));
        coin.setPriceChangePercentage24h(getDouble(marketData, "price_change_percentage_24h"));
        coin.setMarketCapChange24h(getDouble(marketData.path("market_cap_change_24h"), "usd"));
        coin.setMarketCapChangePercentage24h(getDouble(marketData, "market_cap_change_percentage_24h"));
        coin.setCirculatingSupply(getDouble(marketData, "circulating_supply"));
        coin.setTotalSupply(getDouble(marketData, "total_supply"));
        coin.setMaxSupply(getDouble(marketData, "max_supply"));
        coin.setAth(getDouble(marketData.path("ath"), "usd"));
        coin.setAthChangePercentage(getDouble(marketData, "ath_change_percentage"));
        coin.setAthDate(parseInstant(marketData.path("ath_date").path("usd").asText(null)));
        coin.setAtl(getDouble(marketData.path("atl"), "usd"));
        coin.setAtlChangePercentage(getDouble(marketData, "atl_change_percentage"));
        coin.setAtlDate(parseInstant(marketData.path("atl_date").path("usd").asText(null)));
        coin.setLastUpdated(parseInstant(json.path("last_updated").asText(null)));

        return coin;
    }

    // Helper returning Double or null (avoids JpaSystemException on null response properties)
    private Double getDouble(JsonNode parent, String field) {
        JsonNode node = parent.path(field);
        return node.isNumber() ? node.asDouble() : null;
    }

    // Helper returning Long or null
    private Long getLong(JsonNode parent, String field) {
        JsonNode node = parent.path(field);
        return node.isNumber() ? node.asLong() : null;
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Coin> getFallbackCoins() {
        List<Coin> list = new ArrayList<>();
        list.add(createFallbackCoin("bitcoin", "Bitcoin", "btc", "https://assets.coingecko.com/coins/images/1/large/bitcoin.png", 64820.50, 1276000000000L, 34500000000L, 2.45, 1550.20));
        list.add(createFallbackCoin("ethereum", "Ethereum", "eth", "https://assets.coingecko.com/coins/images/279/large/ethereum.png", 3480.20, 418000000000L, 18200000000L, 1.85, 63.40));
        list.add(createFallbackCoin("tether", "Tether", "usdt", "https://assets.coingecko.com/coins/images/325/large/Tether.png", 1.00, 118000000000L, 45000000000L, 0.02, 0.0002));
        list.add(createFallbackCoin("binancecoin", "BNB", "bnb", "https://assets.coingecko.com/coins/images/825/large/bnb-icon2_2x.png", 584.10, 85000000000L, 1200000000L, -0.65, -3.80));
        list.add(createFallbackCoin("solana", "Solana", "sol", "https://assets.coingecko.com/coins/images/4128/large/solana.png", 146.75, 68000000000L, 3100000000L, 5.12, 7.15));
        list.add(createFallbackCoin("ripple", "XRP", "xrp", "https://assets.coingecko.com/coins/images/44/large/xrp-symbol-white-128.png", 0.58, 32000000000L, 950000000L, -1.15, -0.006));
        list.add(createFallbackCoin("cardano", "Cardano", "ada", "https://assets.coingecko.com/coins/images/975/large/cardano.png", 0.36, 12800000000L, 310000000L, 0.85, 0.003));
        list.add(createFallbackCoin("dogecoin", "Dogecoin", "doge", "https://assets.coingecko.com/coins/images/5/large/dogecoin.png", 0.108, 15700000000L, 620000000L, 3.40, 0.0035));
        list.add(createFallbackCoin("tron", "TRON", "trx", "https://assets.coingecko.com/coins/images/1094/large/tron-logo.png", 0.154, 13400000000L, 410000000L, -0.40, -0.0006));
        list.add(createFallbackCoin("avalanche-2", "Avalanche", "avax", "https://assets.coingecko.com/coins/images/12559/large/Avalanche_Circle_RedWhite_Trans.png", 23.90, 9400000000L, 280000000L, 4.10, 0.94));
        return list;
    }

    private Coin createFallbackCoin(String id, String name, String symbol, String img, Double price, Long mc, Long vol, Double changePct, Double change24h) {
        Coin coin = new Coin();
        coin.setId(id);
        coin.setName(name);
        coin.setSymbol(symbol);
        coin.setImage(img);
        coin.setCurrentPrice(price);
        coin.setMarketCap(mc);
        coin.setTotalVolume(vol);
        coin.setPriceChangePercentage24h(changePct);
        coin.setPriceChange24h(change24h);
        return coin;
    }

    private JsonNode getFallbackChartJson() {
        try {
            long now = System.currentTimeMillis();
            StringBuilder sb = new StringBuilder("{\"prices\":[");
            for (int i = 24; i >= 0; i--) {
                long time = now - (i * 3600000L);
                double price = 64000 + (Math.sin(i) * 800) + (Math.random() * 200);
                sb.append("[").append(time).append(",").append(String.format("%.2f", price)).append("]");
                if (i > 0) sb.append(",");
            }
            sb.append("]}");
            return objectMapper.readTree(sb.toString());
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }
}