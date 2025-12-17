package com.gm.expensight.service;

import com.gm.expensight.exception.LlmException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LlmServiceFactory {
    
    private final List<LlmService> llmServices;
    private final Map<String, LlmService> serviceMap;
    private final String defaultProvider;
    
    public LlmServiceFactory(List<LlmService> llmServices,
                            @Value("${llm.provider:openrouter}") String defaultProvider) {
        this.llmServices = llmServices;
        this.serviceMap = llmServices.stream()
                .collect(Collectors.toMap(
                        LlmService::getProviderName,
                        Function.identity(),
                        (existing, replacement) -> {
                            log.warn("Duplicate LLM provider name: {}. Using first occurrence.", existing.getProviderName());
                            return existing;
                        }
                ));
        this.defaultProvider = defaultProvider;
        log.info("Initialized LLM Service Factory with {} providers: {} (default: {})", 
                llmServices.size(), 
                llmServices.stream().map(LlmService::getProviderName).collect(Collectors.joining(", ")),
                defaultProvider);
    }
    
    public LlmService getDefaultLlmService() {
        return getLlmService(defaultProvider);
    }
    
    public LlmService getLlmService(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            return getDefaultLlmService();
        }
        
        String normalizedName = providerName.toLowerCase();
        LlmService service = serviceMap.values().stream()
                .filter(s -> s.getProviderName().toLowerCase().equals(normalizedName))
                .findFirst()
                .orElse(null);
        
        if (service != null && service.isAvailable()) {
            return service;
        }
        
        log.warn("LLM provider '{}' not found or not available. Falling back to default.", providerName);
        return getFirstAvailableService();
    }
    
    public LlmService getFirstAvailableService() {
        return llmServices.stream()
                .filter(LlmService::isAvailable)
                .findFirst()
                .orElseThrow(() -> new LlmException(
                        "No LLM service is available. Please check LLM configuration."));
    }
}

