package com.gm.expensight.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gm.expensight.exception.LlmException;
import com.gm.expensight.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

@Slf4j
@Service
@ConditionalOnProperty(name = "llm.provider", havingValue = "openrouter", matchIfMissing = true)
public class OpenRouterLlmService implements LlmService {
    
    private static final String OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    
    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;
    
    public OpenRouterLlmService(
            @Value("${llm.openrouter.api-key:}") String apiKey,
            @Value("${llm.openrouter.model:openai/gpt-4o-mini}") String model,
            WebClient.Builder webClientBuilder) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = new ObjectMapper();
        
        this.webClient = webClientBuilder
                .baseUrl(OPENROUTER_API_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("HTTP-Referer", "https://github.com/expensight")
                .defaultHeader("X-Title", "Expensight")
                .build();
    }
    
    @Override
    public String generateText(String prompt) throws LlmException {
        return generateText(prompt, 0.7);
    }
    
    @Override
    public String generateText(String prompt, double temperature) throws LlmException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new LlmException("OpenRouter API key is not configured. Set llm.openrouter.api-key property.");
        }
        
        try {
            String requestBody = buildRequestBody(prompt, temperature);
            log.debug("Sending request to OpenRouter API (model: {}, prompt length: {})", model, prompt.length());
            
            String response = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();
            
            String content = extractContentFromResponse(response);
            log.debug("Received response from OpenRouter ({} characters)", content.length());
            
            return content;
            
        } catch (WebClientResponseException e) {
            log.error("OpenRouter API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new LlmException("OpenRouter API error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to call OpenRouter API: {}", e.getMessage(), e);
            throw new LlmException("Failed to call OpenRouter API: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
    
    @Override
    public String getProviderName() {
        return "OpenRouter";
    }
    
    private String buildRequestBody(String prompt, double temperature) {
        try {
            return objectMapper.writeValueAsString(new OpenRouterRequest(
                    model,
                    new Message[] { new Message("user", prompt) },
                    temperature
            ));
        } catch (Exception e) {
            throw new LlmException("Failed to build request body", e);
        }
    }
    
    private String extractContentFromResponse(String response) throws LlmException {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).path("message");
                String content = message.path("content").asText();
                if (content == null || content.trim().isEmpty()) {
                    throw new LlmException("Empty response from OpenRouter API");
                }
                return content.trim();
            }
            throw new LlmException("Invalid response format from OpenRouter API");
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("Failed to parse OpenRouter response: " + e.getMessage(), e);
        }
    }
    
    private record OpenRouterRequest(String model, Message[] messages, double temperature) {}
    private record Message(String role, String content) {}
}

