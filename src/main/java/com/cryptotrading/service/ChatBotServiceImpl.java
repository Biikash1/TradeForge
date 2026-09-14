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

    private static final String SYSTEM_INSTRUCTION =
            "You are an assistant for a cryptocurrency trading platform. " +
                    "Respond in clean plain text with standard spacing. " +
                    "Do not include Markdown symbols, asterisks for bolding, or headers.";

    @Override
    public ApiResponse getCoinDetails(String prompt) throws Exception {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt cannot be empty");
        }

        String geminiApiUrl = baseUrl + "/models/" + modelName + ":generateContent?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 1. Declare tool schema
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
                                                        .put("description", "The cryptocurrency name or symbol, e.g. bitcoin, ethereum, solana, btc, eth")
                                                )
                                        )
                                        .put("required", new JSONArray().put("currencyName"))
                                )
                        )
                );

        // 2. Turn 1: User prompt with tools
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
        JSONObject candidate = firstResponseJson.getJSONArray("candidates").getJSONObject(0);

        // Retain original model content turn to preserve thought_signature
        JSONObject modelContentTurn = candidate.getJSONObject("content");
        JSONArray parts = modelContentTurn.getJSONArray("parts");
        JSONObject firstPart = parts.getJSONObject(0);

        // If Gemini answers directly without calling a tool
        if (!firstPart.has("functionCall")) {
            return new ApiResponse(sanitizeOutput(firstPart.optString("text", "")));
        }

        // 3. Extract parameter & fetch live CoinGecko data
        JSONObject functionCall = firstPart.getJSONObject("functionCall");
        String rawCurrencyName = functionCall.getJSONObject("args").getString("currencyName");
        String normalizedCoinId = normalizeCoinName(rawCurrencyName);

        Coin coin = makeApiRequest(normalizedCoinId);
        JSONObject coinJson = new JSONObject(objectMapper.writeValueAsString(coin));

        // 4. Turn 2: Return function result with concise answer instruction
        JSONObject functionResponsePart = new JSONObject()
                .put("functionResponse", new JSONObject()
                        .put("name", "getCoinDetails")
                        .put("response", new JSONObject().put("result", coinJson))
                );

        JSONObject shortAnswerInstruction = new JSONObject()
                .put("text", "Based on the coin data provided, reply directly in ONE short sentence answering: '"
                        + prompt + "'. Format like: The current value of [Coin] is [Price]. Do not add any extra explanations.");

        JSONObject userResponseTurn = new JSONObject()
                .put("role", "user")
                .put("parts", new JSONArray()
                        .put(functionResponsePart)
                        .put(shortAnswerInstruction)
                );

        JSONArray conversationContents = new JSONArray()
                .put(new JSONObject()
                        .put("role", "user")
                        .put("parts", new JSONArray().put(new JSONObject().put("text", prompt))))
                .put(modelContentTurn)
                .put(userResponseTurn);

        JSONObject secondTurnBody = new JSONObject().put("contents", conversationContents);

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
                .getString("text");

        return new ApiResponse(sanitizeOutput(finalAnswer));
    }

    @Override
    public String simpleChat(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt must not be empty");
        }

        GeminiDto.Request requestPayload = GeminiDto.Request.of(prompt.trim(), SYSTEM_INSTRUCTION);

        try {
            String rawJson = geminiRestClient.post()
                    .uri("/models/{model}:generateContent", modelName)
                    .body(requestPayload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        String errorBody = new String(res.getBody().readAllBytes());
                        log.error("Gemini upstream failure [{}]: {}", res.getStatusCode(), errorBody);
                        throw new GeminiApiException("Gemini upstream API returned error: " + errorBody, res.getStatusCode().value());
                    })
                    .body(String.class);

            if (rawJson == null || rawJson.isBlank()) {
                throw new GeminiApiException("Received empty response from Gemini API", 502);
            }

            // Using tools.jackson.databind.ObjectMapper or JsonNode tree
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

        } catch (GeminiApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing Gemini response", e);
            throw new GeminiApiException("Failed to parse response: " + e.getMessage(), 500);
        }
    }

    public Coin makeApiRequest(String currencyName) throws Exception {
        String url = "https://api.coingecko.com/api/v3/coins/" + currencyName;

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

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
        throw new IllegalArgumentException("Coin not found: " + currencyName);
    }

    private String normalizeCoinName(String name) {
        if (name == null) return "bitcoin";
        String lower = name.trim().toLowerCase();
        return switch (lower) {
            case "btc" -> "bitcoin";
            case "eth", "etherium" -> "ethereum";
            case "sol" -> "solana";
            case "doge" -> "dogecoin";
            case "ada" -> "cardano";
            case "xrp", "ripple" -> "ripple";
            case "dot" -> "polkadot";
            default -> lower.replaceAll("[^a-z0-9-]", "");
        };
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