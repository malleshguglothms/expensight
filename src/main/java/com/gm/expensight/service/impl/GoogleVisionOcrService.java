package com.gm.expensight.service.impl;

import com.gm.expensight.service.OcrException;
import com.gm.expensight.service.OcrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "ocr.provider", havingValue = "google-vision", matchIfMissing = false)
public class GoogleVisionOcrService implements OcrService {
    
    @Override
    public String extractText(byte[] imageData) throws OcrException {
        log.warn("Google Vision OCR not yet implemented.");
        throw new OcrException("Google Vision OCR is not yet implemented. " +
                "Please use Tesseract (ocr.provider=tesseract) for now.");
    }
    
    @Override
    public String extractTextFromPdf(byte[] pdfData) throws OcrException {
        log.warn("Google Vision PDF OCR not yet implemented.");
        throw new OcrException("Google Vision PDF OCR is not yet implemented.");
    }
    
    @Override
    public String getProviderName() {
        return "Google Vision";
    }
    
    @Override
    public boolean isAvailable() {
        return false;
    }
}

