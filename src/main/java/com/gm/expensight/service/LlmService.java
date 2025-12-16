package com.gm.expensight.service;

import com.gm.expensight.exception.LlmException;

public interface LlmService {
    
    String generateText(String prompt) throws LlmException;
    
    String generateText(String prompt, double temperature) throws LlmException;
    
    default boolean isAvailable() {
        return true;
    }
    
    String getProviderName();
}

