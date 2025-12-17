package com.gm.expensight.config;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class OcrConfig {
    
    @Bean
    public Tesseract tesseract(@Value("${ocr.tesseract.data-path:}") String tessDataPath) {
        Tesseract tesseract = new Tesseract();
        
        if (tessDataPath != null && !tessDataPath.trim().isEmpty()) {
            tesseract.setDatapath(tessDataPath);
            log.info("Tesseract data path set to: {}", tessDataPath);
        }
        
        tesseract.setLanguage("eng");
        tesseract.setPageSegMode(1);
        tesseract.setOcrEngineMode(3);
        
        return tesseract;
    }
}

