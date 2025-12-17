package com.gm.expensight.service.impl;

import com.gm.expensight.domain.model.FileMetadata;
import com.gm.expensight.domain.model.ProcessingStatus;
import com.gm.expensight.domain.model.Receipt;
import com.gm.expensight.repository.ReceiptRepository;
import com.gm.expensight.service.FileStorageService;
import com.gm.expensight.service.FileValidator;
import com.gm.expensight.exception.LlmException;
import com.gm.expensight.exception.OcrException;
import com.gm.expensight.exception.ResourceNotFoundException;
import com.gm.expensight.service.OcrService;
import com.gm.expensight.service.OcrServiceFactory;
import com.gm.expensight.service.ReceiptParserService;
import com.gm.expensight.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    private final FileValidator fileValidator;
    private final FileStorageService fileStorageService;
    private final ReceiptRepository receiptRepository;
    private final OcrServiceFactory ocrServiceFactory;
    private final ReceiptParserService receiptParserService;

    @Override
    @Transactional
    public Receipt uploadReceipt(MultipartFile file, String userEmail) {
        log.info("Uploading receipt for user: {}", userEmail);
        
        fileValidator.validate(file);
        String storagePath = fileStorageService.storeFile(file, userEmail);
        
        FileMetadata fileMetadata = FileMetadata.builder()
                .id(UUID.randomUUID())
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .storagePath(storagePath)
                .uploadedAt(LocalDateTime.now())
                .build();
        
        Receipt receipt = Receipt.builder()
                .userEmail(userEmail)
                .merchantName("Unknown")
                .totalAmount(BigDecimal.ZERO)
                .receiptDate(LocalDate.now())
                .taxAmount(BigDecimal.ZERO)
                .currency("INR")
                .status(ProcessingStatus.PENDING)
                .fileMetadata(fileMetadata)
                .build();
        
        Receipt savedReceipt = receiptRepository.save(receipt);
        log.info("Receipt created with ID: {}", savedReceipt.getId());
        
        try {
            processReceipt(savedReceipt.getId());
            log.info("Receipt {} processed successfully after upload", savedReceipt.getId());
        } catch (Exception e) {
            log.warn("Auto-processing failed for receipt {}: {}. Receipt saved but not processed. " +
                    "Status: PENDING. Can be retried manually.", savedReceipt.getId(), e.getMessage());
        }
        
        return savedReceipt;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Receipt> getUserReceipts(String userEmail) {
        log.debug("Retrieving receipts for user: {}", userEmail);
        return receiptRepository.findByUserEmailOrderByCreatedAtDesc(userEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public Receipt getReceiptById(UUID receiptId) {
        log.debug("Retrieving receipt with ID: {}", receiptId);
        return receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt", receiptId));
    }

    @Override
    @Transactional
    public Receipt processReceipt(UUID receiptId) {
        log.info("Processing receipt with ID: {}", receiptId);
        
        Receipt receipt = getReceiptById(receiptId);
        
        if (receipt.getFileMetadata() == null || receipt.getFileMetadata().getStoragePath() == null) {
            log.error("Receipt {} has no file metadata or storage path", receiptId);
            receipt.setStatus(ProcessingStatus.FAILED);
            receipt.setFailureReason("Missing file metadata or storage path");
            return receiptRepository.save(receipt);
        }

        try {
            OcrService ocrService = ocrServiceFactory.getDefaultOcrService();
            if (!ocrService.isAvailable()) {
                throw new OcrException("OCR service is not available");
            }

            byte[] fileData = fileStorageService.loadFile(receipt.getFileMetadata().getStoragePath());
            String contentType = receipt.getFileMetadata().getContentType();

            String extractedText;
            if (contentType != null && contentType.equals("application/pdf")) {
                extractedText = ocrService.extractTextFromPdf(fileData);
            } else {
                extractedText = ocrService.extractText(fileData);
            }

            receipt.setRawOcrText(extractedText);
            receipt.setStatus(ProcessingStatus.PROCESSING);
            
            log.info("OCR completed for receipt {}. Extracted {} characters.", 
                    receiptId, extractedText != null ? extractedText.length() : 0);
            
            receiptRepository.save(receipt);
            
            try {
                log.debug("Starting LLM parsing for receipt {}", receiptId);
                var parsingResult = receiptParserService.parseReceipt(extractedText);
                
                Receipt receiptToUpdate = receiptRepository.findById(receiptId)
                        .orElseThrow(() -> new ResourceNotFoundException("Receipt", receiptId));
                
                receiptParserService.applyParsingResult(receiptToUpdate, parsingResult);
                receiptToUpdate.setStatus(ProcessingStatus.COMPLETED);
                log.info("LLM parsing completed for receipt {}. Extracted merchant: {}, total: {}", 
                        receiptId, parsingResult.getMerchantName(), parsingResult.getTotalAmount());
                
                return receiptRepository.save(receiptToUpdate);
            } catch (LlmException e) {
                log.error("LLM parsing failed for receipt {}: {}", receiptId, e.getMessage(), e);
                Receipt failedReceipt = receiptRepository.findById(receiptId)
                        .orElseThrow(() -> new ResourceNotFoundException("Receipt", receiptId));
                failedReceipt.setStatus(ProcessingStatus.FAILED);
                failedReceipt.setFailureReason("LLM parsing failed: " + e.getMessage());
                return receiptRepository.save(failedReceipt);
            }

        } catch (OcrException e) {
            log.error("OCR processing failed for receipt {}: {}", receiptId, e.getMessage(), e);
            receipt.setStatus(ProcessingStatus.FAILED);
            receipt.setFailureReason("OCR processing failed: " + e.getMessage());
            return receiptRepository.save(receipt);
        } catch (Exception e) {
            log.error("Unexpected error processing receipt {}: {}", receiptId, e.getMessage(), e);
            receipt.setStatus(ProcessingStatus.FAILED);
            receipt.setFailureReason("Processing error: " + e.getMessage());
            return receiptRepository.save(receipt);
        }
    }
}

