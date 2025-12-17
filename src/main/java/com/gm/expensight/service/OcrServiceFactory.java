package com.gm.expensight.service;

import com.gm.expensight.exception.OcrException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OcrServiceFactory {

    private final List<OcrService> ocrServices;
    private final Map<String, OcrService> serviceMap;
    private final String defaultProvider;

    public OcrServiceFactory(List<OcrService> ocrServices, 
                            @Value("${ocr.provider:tesseract}") String defaultProvider) {
        this.ocrServices = ocrServices;
        this.serviceMap = ocrServices.stream()
                .collect(Collectors.toMap(
                        OcrService::getProviderName,
                        Function.identity(),
                        (existing, replacement) -> {
                            log.warn("Duplicate OCR provider name: {}. Using first occurrence.", existing.getProviderName());
                            return existing;
                        }
                ));
        this.defaultProvider = defaultProvider;
        log.info("Initialized OCR Service Factory with {} providers: {} (default: {})", 
                ocrServices.size(), 
                ocrServices.stream().map(OcrService::getProviderName).collect(Collectors.joining(", ")),
                defaultProvider);
    }

    public OcrService getDefaultOcrService() {
        return getOcrService(defaultProvider);
    }

    public OcrService getOcrService(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            return getDefaultOcrService();
        }

        String normalizedName = providerName.toLowerCase();
        OcrService service = serviceMap.values().stream()
                .filter(s -> s.getProviderName().toLowerCase().equals(normalizedName))
                .findFirst()
                .orElse(null);

        if (service != null && service.isAvailable()) {
            return service;
        }

        log.warn("OCR provider '{}' not found or not available. Falling back to default.", providerName);
        return getFirstAvailableService();
    }

    public OcrService getFirstAvailableService() {
        return ocrServices.stream()
                .filter(OcrService::isAvailable)
                .findFirst()
                .orElseThrow(() -> new OcrException(
                        "No OCR service is available. Please check OCR configuration."));
    }

    public List<OcrService> getAvailableServices() {
        return ocrServices.stream()
                .filter(OcrService::isAvailable)
                .collect(Collectors.toList());
    }

    public boolean hasAvailableService() {
        return ocrServices.stream().anyMatch(OcrService::isAvailable);
    }
}

