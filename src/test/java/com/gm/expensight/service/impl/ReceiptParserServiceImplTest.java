package com.gm.expensight.service.impl;

import com.gm.expensight.domain.model.Receipt;
import com.gm.expensight.exception.LlmException;
import com.gm.expensight.service.LlmService;
import com.gm.expensight.service.PromptService;
import com.gm.expensight.service.ReceiptParserService;
import com.gm.expensight.service.dto.ReceiptParsingResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiptParserServiceImplTest {
    
    @Mock
    private LlmService llmService;
    
    @Mock
    private PromptService promptService;
    
    private ReceiptParserService receiptParserService;
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        receiptParserService = new ReceiptParserServiceImpl(llmService, promptService, objectMapper);
    }
    
    @Test
    void shouldHandleJsonWithCodeBlocks() throws LlmException {
        String ocrText = "Sample receipt text";
        String prompt = "Parse this receipt...";
        String llmResponse = "```json\n{\"merchantName\":\"Store\",\"totalAmount\":100.0,\"receiptDate\":\"2024-12-16\",\"currency\":\"INR\",\"items\":[]}\n```";
        
        when(promptService.buildReceiptParsingPrompt(ocrText)).thenReturn(prompt);
        when(llmService.generateText(prompt, 0.2)).thenReturn(llmResponse);
        
        ReceiptParsingResult result = receiptParserService.parseReceipt(ocrText);
        
        assertThat(result).isNotNull();
        assertThat(result.getMerchantName()).isEqualTo("Store");
    }
    
    @Test
    void shouldParseReceiptSuccessfully() throws LlmException {
        String ocrText = "Sample receipt text from OCR";
        String prompt = "Parse this receipt...";
        String llmResponse = """
            {
              "merchantName": "Test Store",
              "totalAmount": 150.50,
              "receiptDate": "2024-12-16",
              "taxAmount": 15.05,
              "currency": "INR",
              "items": [
                {"itemName": "Item 1", "quantity": 2, "price": 50.00},
                {"itemName": "Item 2", "quantity": 1, "price": 50.50}
              ]
            }
            """;
        
        when(promptService.buildReceiptParsingPrompt(ocrText)).thenReturn(prompt);
        when(llmService.generateText(prompt, 0.2)).thenReturn(llmResponse);
        
        ReceiptParsingResult result = receiptParserService.parseReceipt(ocrText);
        
        assertThat(result).isNotNull();
        assertThat(result.getMerchantName()).isEqualTo("Test Store");
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(150.50));
        assertThat(result.getItems()).hasSize(2);
        
        verify(promptService).buildReceiptParsingPrompt(ocrText);
        verify(llmService).generateText(prompt, 0.2);
    }
    
    @Test
    void shouldHandleLlmException() throws LlmException {
        String ocrText = "Sample OCR text";
        String prompt = "Parse this receipt...";
        
        when(promptService.buildReceiptParsingPrompt(ocrText)).thenReturn(prompt);
        when(llmService.generateText(prompt, 0.2)).thenThrow(new LlmException("LLM API error"));
        
        assertThatThrownBy(() -> receiptParserService.parseReceipt(ocrText))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("LLM API error");
    }
    
    @Test
    void shouldApplyParsingResultToReceipt() {
        Receipt receipt = Receipt.builder()
                .id(java.util.UUID.randomUUID())
                .userEmail("test@example.com")
                .merchantName("Unknown")
                .totalAmount(BigDecimal.ZERO)
                .receiptDate(LocalDate.now())
                .build();
        
        ReceiptParsingResult parsingResult = ReceiptParsingResult.builder()
                .merchantName("Parsed Store")
                .totalAmount(BigDecimal.valueOf(200.00))
                .receiptDate(LocalDate.of(2024, 12, 16))
                .taxAmount(BigDecimal.valueOf(20.00))
                .currency("INR")
                .items(List.of(
                        ReceiptParsingResult.ReceiptItemDto.builder()
                                .itemName("Product A")
                                .quantity(2)
                                .price(BigDecimal.valueOf(90.00))
                                .build()
                ))
                .build();
        
        Receipt updated = receiptParserService.applyParsingResult(receipt, parsingResult);
        
        assertThat(updated.getMerchantName()).isEqualTo("Parsed Store");
        assertThat(updated.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(200.00));
        assertThat(updated.getReceiptDate()).isEqualTo(LocalDate.of(2024, 12, 16));
        assertThat(updated.getTaxAmount()).isEqualByComparingTo(BigDecimal.valueOf(20.00));
        assertThat(updated.getCurrency()).isEqualTo("INR");
        assertThat(updated.getItems()).hasSize(1);
        assertThat(updated.getItems().get(0).getItemName()).isEqualTo("Product A");
    }
}

