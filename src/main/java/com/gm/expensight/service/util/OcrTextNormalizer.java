package com.gm.expensight.service.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
public class OcrTextNormalizer {
    
    private static final Pattern COMMON_CURRENCY_MISREADS = Pattern.compile(
            "(?i)\\bJPY\\b|\\b¥\\b|\\b\\u00A5\\b"
    );
    
    public String normalizeCurrencySymbols(String ocrText) {
        if (ocrText == null || ocrText.trim().isEmpty()) {
            return ocrText;
        }
        
        String normalized = ocrText;
        
        normalized = COMMON_CURRENCY_MISREADS.matcher(normalized).replaceAll("₹");
        
        normalized = normalized.replaceAll("(?i)\\bJPY\\b", "INR");
        normalized = normalized.replaceAll("(?i)\\b¥\\b", "₹");
        normalized = normalized.replaceAll("\\u00A5", "₹");
        
        normalized = normalized.replaceAll("(?i)\\bRs\\.?\\s*", "₹");
        normalized = normalized.replaceAll("(?i)\\bINR\\s*", "₹");
        
        if (normalized.contains("₹") && !normalized.contains("INR")) {
            log.debug("Normalized currency symbols in OCR text (found ₹ symbol)");
        }
        
        return normalized;
    }
    
    public String enhanceForIndianReceipts(String ocrText) {
        if (ocrText == null || ocrText.trim().isEmpty()) {
            return ocrText;
        }
        
        String enhanced = normalizeCurrencySymbols(ocrText);
        
        enhanced = enhanced.replaceAll("(?i)\\bGST\\b", "GST");
        enhanced = enhanced.replaceAll("(?i)\\bCGST\\b", "CGST");
        enhanced = enhanced.replaceAll("(?i)\\bSGST\\b", "SGST");
        
        return enhanced;
    }
}

