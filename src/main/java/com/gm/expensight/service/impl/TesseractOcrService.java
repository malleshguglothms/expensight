package com.gm.expensight.service.impl;

import com.gm.expensight.service.OcrException;
import com.gm.expensight.service.OcrService;
import com.gm.expensight.service.util.PdfToImageConverter;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class TesseractOcrService implements OcrService {
    
    private static final String PAGE_SEPARATOR = "\n--- Page %d ---\n";
    
    private final Tesseract tesseract;
    private final PdfToImageConverter pdfConverter;
    
    public TesseractOcrService(Tesseract tesseract, PdfToImageConverter pdfConverter) {
        this.tesseract = tesseract;
        this.pdfConverter = pdfConverter;
    }
    
    @Override
    public boolean isAvailable() {
        try {
            java.awt.image.BufferedImage testImage = new java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB);
            tesseract.doOCR(testImage);
            return true;
        } catch (UnsatisfiedLinkError e) {
            log.error("Tesseract native library not found. Please install Tesseract and set library path. " +
                    "See TESSERACT_SETUP.md for instructions. Error: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Tesseract OCR is not available: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public String extractText(byte[] imageData) throws OcrException {
        if (imageData == null) {
            throw new IllegalArgumentException("Image data cannot be null");
        }
        
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image == null) {
                throw new OcrException("Failed to read image from provided data");
            }
            
            String text = tesseract.doOCR(image);
            return text != null ? text.trim() : "";
            
        } catch (TesseractException e) {
            log.error("Tesseract OCR failed: {}", e.getMessage(), e);
            throw new OcrException("OCR processing failed: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("Failed to read image: {}", e.getMessage(), e);
            throw new OcrException("Failed to read image: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String extractTextFromPdf(byte[] pdfData) throws OcrException {
        if (pdfData == null) {
            throw new IllegalArgumentException("PDF data cannot be null");
        }
        
        try {
            List<java.awt.image.BufferedImage> pages = pdfConverter.convertToImages(pdfData);
            
            if (pages.isEmpty()) {
                return "";
            }
            
            StringBuilder fullText = new StringBuilder();
            
            for (int i = 0; i < pages.size(); i++) {
                BufferedImage pageImage = pages.get(i);
                
                try {
                    String pageText = tesseract.doOCR(pageImage);
                    if (pageText != null && !pageText.trim().isEmpty()) {
                        if (i > 0) {
                            fullText.append(String.format(PAGE_SEPARATOR, i + 1));
                        }
                        fullText.append(pageText.trim());
                    }
                } catch (TesseractException e) {
                    log.warn("Failed to extract text from page {}: {}", i + 1, e.getMessage());
                }
            }
            
            return fullText.toString();
            
        } catch (IOException e) {
            log.error("Failed to convert PDF to images: {}", e.getMessage(), e);
            throw new OcrException("Failed to convert PDF to images: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getProviderName() {
        return "Tesseract";
    }
}
