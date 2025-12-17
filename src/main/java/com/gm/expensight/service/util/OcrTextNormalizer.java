package com.gm.expensight.service.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OcrTextNormalizer {
    
    public String enhanceForIndianReceipts(String ocrText) {
        if (ocrText == null || ocrText.trim().isEmpty()) {
            return ocrText;
        }
        
        String normalized = ocrText;
        
        normalized = normalized.replaceAll("(?i)\\bRs\\.?\\s*", "₹");
        normalized = normalized.replaceAll("(?i)\\bINR\\s*", "₹");
        
        if (normalized.contains("₹") && !normalized.contains("INR")) {
            log.debug("Normalized currency symbols in OCR text (found ₹ symbol)");
        }
        
        return normalized;
    }
}

