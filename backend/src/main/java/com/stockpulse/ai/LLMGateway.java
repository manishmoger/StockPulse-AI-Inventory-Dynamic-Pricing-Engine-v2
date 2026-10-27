package com.stockpulse.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class LLMGateway {

    private static final Logger log = LoggerFactory.getLogger(LLMGateway.class);

    @Value("${ai.provider:zycus}")
    private String provider;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.model:qwen-cursor}")
    private String model;

    @Value("${ai.base-url:https://litellm-qc.zycus.net/v1/chat/completions}")
    private String baseUrl;

    @Value("${ai.product-header:PC1}")
    private String productHeader;

    @Value("${ai.cookie:}")
    private String cookie;

    @Value("${ai.timeout-seconds:30}")
    private int timeoutSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getProvider() {
        return provider;
    }

    public String complete(String prompt) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("AI provider not configured — API key missing");
        }

        String url = resolveUrl();
        String requestBody = buildRequestBody(prompt);

        log.info("Sending request to AI provider: {}, model: {}", provider, model);

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(timeoutSeconds));

        // Zycus LiteLLM gateway requires a product header (and optional session cookie)
        if ("zycus".equalsIgnoreCase(provider)) {
            reqBuilder.header("product", productHeader);
            if (cookie != null && !cookie.isBlank()) {
                reqBuilder.header("Cookie", cookie);
            }
        }

        HttpRequest request = reqBuilder
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("AI provider returned status {}", response.statusCode());
            throw new RuntimeException("AI provider error: HTTP " + response.statusCode());
        }

        return extractContent(response.body());
    }

    private String resolveUrl() {
        return switch (provider.toLowerCase(Locale.ROOT)) {
            case "zycus"  -> baseUrl;
            case "groq"   -> "https://api.groq.com/openai/v1/chat/completions";
            case "gemini" -> "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent?key=" + apiKey;
            default       -> baseUrl; // treat unknown as custom OpenAI-compatible endpoint
        };
    }

    private String buildRequestBody(String prompt) throws Exception {
        if ("gemini".equalsIgnoreCase(provider)) {
            return objectMapper.writeValueAsString(Map.of(
                "contents", List.of(Map.of(
                    "parts", List.of(Map.of("text", prompt))
                ))
            ));
        }
        // OpenAI-compatible format (used by Zycus LiteLLM, Groq, and most providers)
        return objectMapper.writeValueAsString(Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content",
                    "You are a commerce pricing and inventory advisor. Always respond with valid JSON only."),
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", 0.3,
            "max_tokens", 800
        ));
    }

    private String extractContent(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        if ("gemini".equalsIgnoreCase(provider)) {
            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0).path("text").asText();
        }
        // OpenAI-compatible response (Zycus LiteLLM, Groq, etc.)
        return root.path("choices").get(0).path("message").path("content").asText();
    }
}
