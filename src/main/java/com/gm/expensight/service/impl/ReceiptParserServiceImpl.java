package com.gm.expensight.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gm.expensight.domain.model.Receipt;
import com.gm.expensight.domain.model.ReceiptItem;
import com.gm.expensight.exception.LlmException;
import com.gm.expensight.exception.ValidationException;
import com.gm.expensight.service.LlmService;
import com.gm.expensight.service.PromptService;
import com.gm.expensight.service.ReceiptParserService;
import com.gm.expensight.service.dto.ReceiptParsingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class ReceiptParserServiceImpl implements ReceiptParserService {
    
    private static final double PARSING_TEMPERATURE = 0.2;
    
    private final LlmService llmService;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    
    public ReceiptParserServiceImpl(LlmService llmService, PromptService promptService, ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.promptService = promptService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public ReceiptParsingResult parseReceipt(String ocrText) throws LlmException {
        if (ocrText == null || ocrText.trim().isEmpty()) {
            throw new ValidationException("OCR text cannot be null or empty");
        }
        
        String prompt = promptService.buildReceiptParsingPrompt(ocrText);
        log.debug("Generated prompt for receipt parsing ({} characters)", prompt.length());
        
        String llmResponse = llmService.generateText(prompt, PARSING_TEMPERATURE);
        log.debug("Received LLM response ({} characters)", llmResponse.length());
        
        String cleanedResponse = cleanJsonResponse(llmResponse);
        
        try {
            ReceiptParsingResult result = objectMapper.readValue(cleanedResponse, ReceiptParsingResult.class);
            validateParsingResult(result);
            log.info("Successfully parsed receipt: merchant={}, total={}, items={}", 
                    result.getMerchantName(), result.getTotalAmount(), result.getItems().size());
            return result;
        } catch (Exception e) {
            log.error("Failed to parse LLM response as JSON: {}. Response: {}", e.getMessage(), cleanedResponse);
            throw new LlmException("Failed to parse LLM response: " + e.getMessage(), e);
        }
    }
    
    private String cleanJsonResponse(String response) {
        String cleaned = response.trim();
        
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        
        return cleaned.trim();
    }
    
    private void validateParsingResult(ReceiptParsingResult result) throws LlmException {
        if (result.getMerchantName() == null || result.getMerchantName().trim().isEmpty()) {
            throw new LlmException("Parsed result missing required field: merchantName");
        }
        if (result.getTotalAmount() == null) {
            throw new LlmException("Parsed result missing required field: totalAmount");
        }
        if (result.getReceiptDate() == null) {
            throw new LlmException("Parsed result missing required field: receiptDate");
        }
    }
    
    @Override
    public Receipt applyParsingResult(Receipt receipt, ReceiptParsingResult parsingResult) {
        receipt.setMerchantName(parsingResult.getMerchantName());
        receipt.setTotalAmount(parsingResult.getTotalAmount());
        receipt.setReceiptDate(parsingResult.getReceiptDate());
        receipt.setTaxAmount(parsingResult.getTaxAmount());
        if (parsingResult.getCurrency() != null) {
            receipt.setCurrency(parsingResult.getCurrency());
        }
        
        if (receipt.getItems() == null) {
            receipt.setItems(new java.util.ArrayList<>());
        }
        
        receipt.getItems().clear();
        for (ReceiptParsingResult.ReceiptItemDto itemDto : parsingResult.getItems()) {
            ReceiptItem item = ReceiptItem.builder()
                    .receipt(receipt)
                    .itemName(itemDto.getItemName())
                    .quantity(itemDto.getQuantity() != null ? itemDto.getQuantity() : 1)
                    .price(itemDto.getPrice())
                    .build();
            receipt.getItems().add(item);
        }
        
        return receipt;
    }
}

