package com.gm.expensight.service.impl;

import com.gm.expensight.exception.LlmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class OpenRouterLlmServiceTest {
    
    @Mock
    private WebClient webClient;
    
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    
    @Mock
    private WebClient.ResponseSpec responseSpec;
    
    private OpenRouterLlmService llmService;
    
    @BeforeEach
    void setUp() {
        llmService = new OpenRouterLlmService("test-api-key", "openai/gpt-4o-mini", 
                WebClient.builder());
        ReflectionTestUtils.setField(llmService, "webClient", webClient);
    }
    
    @Test
    void shouldReturnProviderName() {
        assertThat(llmService.getProviderName()).isEqualTo("OpenRouter");
    }
    
    @Test
    void shouldReturnFalseWhenApiKeyNotConfigured() {
        OpenRouterLlmService service = new OpenRouterLlmService("", "openai/gpt-4o-mini", 
                WebClient.builder());
        assertThat(service.isAvailable()).isFalse();
    }
    
    @Test
    void shouldReturnTrueWhenApiKeyConfigured() {
        assertThat(llmService.isAvailable()).isTrue();
    }
    
    @Test
    void shouldThrowExceptionWhenApiKeyNotSet() {
        OpenRouterLlmService service = new OpenRouterLlmService(null, "openai/gpt-4o-mini", 
                WebClient.builder());
        
        assertThatThrownBy(() -> service.generateText("test prompt"))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("API key is not configured");
    }
}

