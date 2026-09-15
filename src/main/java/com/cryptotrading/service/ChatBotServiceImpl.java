package com.cryptotrading.service;

import com.cryptotrading.dto.ApiResponse;
import com.cryptotrading.dto.GeminiDto;
import com.cryptotrading.exception.GeminiApiException;
import com.cryptotrading.model.Coin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatBotServiceImpl implements ChatBotService {

    private final RestTemplate restTemplate;
    private final RestClient geminiRestClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.base-url}")
    private String baseUrl;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model}")
    private String modelName;

    private static final Map<String, String> COIN_ALIASES = Map.ofEntries(
            // Top Tier
            Map.entry("btc", "bitcoin"),
            Map.entry("bitcoin", "bitcoin"),
            Map.entry("eth", "ethereum"),
            Map.entry("etherium", "ethereum"),
            Map.entry("ethereum", "ethereum"),
            Map.entry("sol", "solana"),
            Map.entry("solana", "solana"),
            Map.entry("bnb", "binancecoin"),
            Map.entry("binance", "binancecoin"),
            Map.entry("binancecoin", "binancecoin"),
            Map.entry("xrp", "ripple"),
            Map.entry("ripple", "ripple"),
            Map.entry("ada", "cardano"),
            Map.entry("cardano", "cardano"),
            Map.entry("doge", "dogecoin"),
            Map.entry("dogecoin", "dogecoin"),

            // Major Altcoins & Layer 1 / Layer 2
            Map.entry("avax", "avalanche-2"),
            Map.entry("avalanche", "avalanche-2"),
            Map.entry("link", "chainlink"),
            Map.entry("chainlink", "chainlink"),
            Map.entry("matic", "matic-network"),
            Map.entry("polygon", "matic-network"),
            Map.entry("pol", "polygon-ecosystem-token"),
            Map.entry("dot", "polkadot"),
            Map.entry("polkadot", "polkadot"),
            Map.entry("trx", "tron"),
            Map.entry("tron", "tron"),
            Map.entry("near", "near"),
            Map.entry("sui", "sui"),
            Map.entry("apt", "aptos"),
            Map.entry("aptos", "aptos"),
            Map.entry("atom", "cosmos"),
            Map.entry("cosmos", "cosmos"),

            // DeFi & Staking
            Map.entry("aave", "aave"),
            Map.entry("uni", "uniswap"),
            Map.entry("uniswap", "uniswap"),
            Map.entry("ldo", "lido-dao"),
            Map.entry("mkr", "maker"),

            // Stablecoins
            Map.entry("usdt", "tether"),
            Map.entry("tether", "tether"),
            Map.entry("usdc", "usd-coin"),
            Map.entry("dai", "dai"),

            // Memecoins
            Map.entry("shib", "shiba-inu"),
            Map.entry("shiba", "shiba-inu"),
            Map.entry("pepe", "pepe")
    );

    private static final String SYSTEM_INSTRUCTION =
            "You are an assistant for a cryptocurrency trading platform. " +
                    "Respond in clean plain text with standard spacing. " +
                    "Do not include Markdown symbols, asterisks for bolding, or headers.";

    @Override
    public ApiResponse getCoinDetails(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return new ApiResponse("Please provide a valid question or crypto name.");
        }

        try {
            String geminiApiUrl = baseUrl + "/models/" + modelName + ":generateContent?key=" + apiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Tool Declaration
            JSONObject toolDeclaration = new JSONObject()
                    .put("functionDeclarations", new JSONArray()
                            .put(new JSONObject()
                                    .put("name", "getCoinDetails")
                                    .put("description", "Get the real-time coin details from given currency name or symbol")
                                    .put("parameters", new JSONObject()
                                            .put("type", "OBJECT")
                                            .put("properties", new JSONObject()
                                                    .put("currencyName", new JSONObject()
                                                            .put("type", "STRING")
                                                            .put("description", "The cryptocurrency name or symbol, e.g. btc, bnb, eth, solana")
                                                    )
                                            )
                                            .put("required", new JSONArray().put("currencyName"))
                                    )
                            )
                    );

            //  User prompt with tool declaration
            JSONObject firstTurnBody = new JSONObject()
                    .put("contents", new JSONArray()
                            .put(new JSONObject()
                                    .put("role", "user")
                                    .put("parts", new JSONArray().put(new JSONObject().put("text", prompt)))))
                    .put("tools", new JSONArray().put(toolDeclaration));

            ResponseEntity<String> firstResponse = restTemplate.postForEntity(
                    geminiApiUrl,
                    new HttpEntity<>(firstTurnBody.toString(), headers),
                    String.class
            );

            JSONObject firstResponseJson = new JSONObject(firstResponse.getBody());
            JSONArray candidates = firstResponseJson.optJSONArray("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return new ApiResponse(simpleChat(prompt));
            }

            JSONObject candidate = candidates.getJSONObject(0);
            JSONObject modelContentTurn = candidate.getJSONObject("content");
            JSONArray parts = modelContentTurn.getJSONArray("parts");
            JSONObject firstPart = parts.getJSONObject(0);

            // If Gemini responds without function call, return direct reply
            if (!firstPart.has("functionCall")) {
                return new ApiResponse(sanitizeOutput(firstPart.optString("text", "No information available.")));
            }

            //  Extract parameters & fetch live Coin data safely
            JSONObject functionCall = firstPart.getJSONObject("functionCall");
            String rawCurrencyName = functionCall.getJSONObject("args").optString("currencyName", prompt);
            String normalizedCoinId = normalizeCoinName(rawCurrencyName);

            Coin coin = makeApiRequest(normalizedCoinId);
            if (coin == null) {
                // Fallback to simpleChat if CoinGecko failed or rate-limited
                return new ApiResponse(simpleChat(prompt));
            }

            JSONObject coinJson = new JSONObject(objectMapper.writeValueAsString(coin));

            //  Standard Gemini functionResponse structure
            JSONObject functionResponsePart = new JSONObject()
                    .put("functionResponse", new JSONObject()
                            .put("name", "getCoinDetails")
                            .put("response", new JSONObject().put("content", coinJson))
                    );

            JSONObject userResponseTurn = new JSONObject()
                    .put("role", "user")
                    .put("parts", new JSONArray().put(functionResponsePart));

            JSONArray conversationContents = new JSONArray()
                    .put(new JSONObject()
                            .put("role", "user")
                            .put("parts", new JSONArray().put(new JSONObject().put("text", prompt))))
                    .put(modelContentTurn)
                    .put(userResponseTurn);

            JSONObject secondTurnBody = new JSONObject()
                    .put("contents", conversationContents)
                    .put("systemInstruction", new JSONObject()
                            .put("parts", new JSONArray().put(new JSONObject().put("text", SYSTEM_INSTRUCTION))));

            ResponseEntity<String> secondResponse = restTemplate.postForEntity(
                    geminiApiUrl,
                    new HttpEntity<>(secondTurnBody.toString(), headers),
                    String.class
            );

            JSONObject secondResponseJson = new JSONObject(secondResponse.getBody());
            String finalAnswer = secondResponseJson.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .optString("text", "Could not retrieve summary.");

            return new ApiResponse(sanitizeOutput(finalAnswer));

        } catch (Exception e) {
            log.error("Error in getCoinDetails: {}", e.getMessage(), e);
            // Fallback graceful degradation instead of 500 error
            try {
                return new ApiResponse(simpleChat(prompt));
            } catch (Exception ex) {
                return new ApiResponse("Unable to process request right now. Please try again shortly.");
            }
        }
    }

    @Override
    public String simpleChat(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "Please ask a question.";
        }

        GeminiDto.Request requestPayload = GeminiDto.Request.of(prompt.trim(), SYSTEM_INSTRUCTION);

        try {
            String rawJson = geminiRestClient.post()
                    .uri("/models/{model}:generateContent", modelName)
                    .body(requestPayload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        String errorBody = new String(res.getBody().readAllBytes());
                        log.error("Gemini API error [{}]: {}", res.getStatusCode(), errorBody);
                        throw new GeminiApiException("Gemini upstream API returned error", res.getStatusCode().value());
                    })
                    .body(String.class);

            if (rawJson == null || rawJson.isBlank()) {
                return "No response from AI service.";
            }

            JsonNode rootNode = objectMapper.readTree(rawJson);
            JsonNode candidatesNode = rootNode.path("candidates");

            if (candidatesNode.isArray() && !candidatesNode.isEmpty()) {
                String output = candidatesNode.get(0)
                        .path("content")
                        .path("parts")
                        .get(0)
                        .path("text")
                        .asText("");
                return sanitizeOutput(output);
            }
            return "";
        } catch (Exception e) {
            log.error("Error in simpleChat: {}", e.getMessage());
            return "Market assistant is momentarily unavailable.";
        }
    }

    public Coin makeApiRequest(String currencyName) {
        try {
            String url = "https://api.coingecko.com/api/v3/coins/" + currencyName;

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.add("User-Agent", "Mozilla/5.0 (TradeForge/1.0; Windows NT 10.0; Win64; x64)");

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> responseBody = responseEntity.getBody();

            if (responseBody != null) {
                Map<String, Object> image = (Map<String, Object>) responseBody.get("image");
                Map<String, Object> marketData = (Map<String, Object>) responseBody.get("market_data");

                Coin coin = new Coin();
                coin.setId((String) responseBody.get("id"));
                coin.setName((String) responseBody.get("name"));
                coin.setSymbol((String) responseBody.get("symbol"));

                if (image != null) {
                    coin.setImage((String) image.get("large"));
                }

                if (marketData != null) {
                    coin.setCurrentPrice(convertToDouble(getNested(marketData, "current_price", "usd")));
                    coin.setMarketCap(convertToLong(getNested(marketData, "market_cap", "usd")));
                    coin.setMarketCapRank(convertToLong(marketData.get("market_cap_rank")));
                    coin.setTotalVolume(convertToLong(getNested(marketData, "total_volume", "usd")));
                    coin.setHigh24h(convertToDouble(getNested(marketData, "high_24h", "usd")));
                    coin.setLow24h(convertToDouble(getNested(marketData, "low_24h", "usd")));
                    coin.setPriceChange24h(convertToDouble(marketData.get("price_change_24h")));
                    coin.setPriceChangePercentage24h(convertToDouble(marketData.get("price_change_percentage_24h")));
                    coin.setMarketCapChange24h(convertToDouble(marketData.get("market_cap_change_24h")));
                    coin.setMarketCapChangePercentage24h(convertToDouble(marketData.get("market_cap_change_percentage_24h")));
                    coin.setCirculatingSupply(convertToDouble(marketData.get("circulating_supply")));
                    coin.setTotalSupply(convertToDouble(marketData.get("total_supply")));
                }

                return coin;
            }
        } catch (Exception e) {
            log.warn("CoinGecko fetch failed for '{}': {}", currencyName, e.getMessage());
        }
        return null;
    }

    private String normalizeCoinName(String name) {
        if (name == null || name.isBlank()) return "bitcoin";

        String cleanName = name.trim().toLowerCase().replaceAll("[^a-z0-9-]", "");

        // 1. Pre-mapped aliases
        if (COIN_ALIASES.containsKey(cleanName)) {
            return COIN_ALIASES.get(cleanName);
        }

        // 2. Dynamic lookup fallback
        try {
            String searchUrl = "https://api.coingecko.com/api/v3/search?query=" + cleanName;
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.add("User-Agent", "Mozilla/5.0 (TradeForge/1.0)");

            ResponseEntity<Map> response = restTemplate.exchange(
                    searchUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            if (response.getBody() != null && response.getBody().containsKey("coins")) {
                List<Map<String, Object>> coins = (List<Map<String, Object>>) response.getBody().get("coins");
                if (!coins.isEmpty()) {
                    return (String) coins.get(0).get("id");
                }
            }
        } catch (Exception e) {
            log.warn("Dynamic lookup failed for '{}'", cleanName);
        }

        return cleanName;
    }

    private Object getNested(Map<String, Object> map, String parentKey, String childKey) {
        if (map != null && map.get(parentKey) instanceof Map<?, ?> subMap) {
            return subMap.get(childKey);
        }
        return null;
    }

    private Double convertToDouble(Object value) {
        if (value instanceof Number num) return num.doubleValue();
        return null;
    }

    private Long convertToLong(Object value) {
        if (value instanceof Number num) return num.longValue();
        return null;
    }

    private String sanitizeOutput(String output) {
        if (output == null) return "";
        return output.replace("**", "").replace("###", "").trim();
    }
}