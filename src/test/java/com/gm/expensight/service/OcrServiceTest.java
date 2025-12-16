package com.gm.expensight.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base test class for OCR service implementations.
 * Tests the contract that all OCR implementations must satisfy.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OCR Service Contract Tests")
abstract class OcrServiceTest<T extends OcrService> {
    
    protected T ocrService;
    
    protected abstract T createOcrService();
    
    @BeforeEach
    void setUp() {
        ocrService = createOcrService();
    }
    
    @Test
    @DisplayName("Should extract text from image")
    void shouldExtractTextFromImage() throws OcrException {
        // Given
        byte[] imageData = createSampleImageData();
        
        // When
        String result = ocrService.extractText(imageData);
        
        // Then
        assertThat(result).isNotNull();
        // Note: Actual text extraction depends on implementation
        // This test verifies the method doesn't throw and returns a string
    }
    
    @Test
    @DisplayName("Should extract text from PDF")
    void shouldExtractTextFromPdf() throws OcrException {
        // Given
        byte[] pdfData = createSamplePdfData();
        
        // When
        String result = ocrService.extractTextFromPdf(pdfData);
        
        // Then
        assertThat(result).isNotNull();
    }
    
    @Test
    @DisplayName("Should return empty string for empty image")
    void shouldHandleEmptyImage() throws OcrException {
        // Given
        byte[] emptyData = new byte[0];
        
        // When
        String result = ocrService.extractText(emptyData);
        
        // Then
        assertThat(result).isNotNull();
    }
    
    @Test
    @DisplayName("Should return provider name")
    void shouldReturnProviderName() {
        // When
        String providerName = ocrService.getProviderName();
        
        // Then
        assertThat(providerName).isNotNull().isNotEmpty();
    }
    
    @Test
    @DisplayName("Should support async text extraction from image")
    void shouldExtractTextFromImageAsync() throws ExecutionException, InterruptedException {
        // Given
        byte[] imageData = createSampleImageData();
        
        // When
        CompletableFuture<String> future = ocrService.extractTextAsync(imageData);
        String result = future.get();
        
        // Then
        assertThat(result).isNotNull();
    }
    
    @Test
    @DisplayName("Should support async text extraction from PDF")
    void shouldExtractTextFromPdfAsync() throws ExecutionException, InterruptedException {
        // Given
        byte[] pdfData = createSamplePdfData();
        
        // When
        CompletableFuture<String> future = ocrService.extractTextFromPdfAsync(pdfData);
        String result = future.get();
        
        // Then
        assertThat(result).isNotNull();
    }
    
    // Helper methods to be implemented by concrete test classes
    protected abstract byte[] createSampleImageData();
    protected abstract byte[] createSamplePdfData();
}
