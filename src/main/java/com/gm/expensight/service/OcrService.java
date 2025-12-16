package com.gm.expensight.service;

import java.util.concurrent.CompletableFuture;

public interface OcrService {
    
    String extractText(byte[] imageData) throws OcrException;
    
    String extractTextFromPdf(byte[] pdfData) throws OcrException;
    
    default CompletableFuture<String> extractTextAsync(byte[] imageData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return extractText(imageData);
            } catch (OcrException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    default CompletableFuture<String> extractTextFromPdfAsync(byte[] pdfData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return extractTextFromPdf(pdfData);
            } catch (OcrException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    String getProviderName();
    
    default boolean isAvailable() {
        return true;
    }
}
