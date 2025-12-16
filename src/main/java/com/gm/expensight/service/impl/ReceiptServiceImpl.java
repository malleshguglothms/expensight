package com.gm.expensight.service.impl;

import com.gm.expensight.domain.model.FileMetadata;
import com.gm.expensight.domain.model.ProcessingStatus;
import com.gm.expensight.domain.model.Receipt;
import com.gm.expensight.repository.ReceiptRepository;
import com.gm.expensight.service.FileStorageService;
import com.gm.expensight.service.FileValidator;
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

/**
 * Implementation of ReceiptService.
 * 
 * Follows Single Responsibility Principle - handles receipt business logic.
 * Orchestrates file validation, storage, and entity creation.
 * 
 * Uses constructor injection for dependencies (Dependency Inversion Principle).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    private final FileValidator fileValidator;
    private final FileStorageService fileStorageService;
    private final ReceiptRepository receiptRepository;

    @Override
    @Transactional
    public Receipt uploadReceipt(MultipartFile file, String userEmail) {
        log.info("Uploading receipt for user: {}", userEmail);
        
        // 1. Validate file
        fileValidator.validate(file);
        
        // 2. Store file
        String storagePath = fileStorageService.storeFile(file, userEmail);
        log.debug("File stored at path: {}", storagePath);
        
        // 3. Create file metadata
        FileMetadata fileMetadata = FileMetadata.builder()
                .id(UUID.randomUUID())
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .storagePath(storagePath)
                .uploadedAt(LocalDateTime.now())
                .build();
        
        // 4. Create receipt entity
        Receipt receipt = Receipt.builder()
                .userEmail(userEmail)
                .merchantName("Unknown") // Will be updated by OCR/LLM processing
                .totalAmount(BigDecimal.ZERO) // Will be updated by OCR/LLM processing
                .receiptDate(LocalDate.now()) // Will be updated by OCR/LLM processing
                .taxAmount(BigDecimal.ZERO)
                .currency("INR")
                .status(ProcessingStatus.PENDING)
                .fileMetadata(fileMetadata)
                .build();
        
        // 5. Save receipt
        Receipt savedReceipt = receiptRepository.save(receipt);
        log.info("Receipt created with ID: {}", savedReceipt.getId());
        
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
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Receipt not found with ID: " + receiptId));
    }

    @Override
    @Transactional
    public Receipt processReceipt(UUID receiptId) {
        log.info("Processing receipt with ID: {}", receiptId);
        
        Receipt receipt = getReceiptById(receiptId);
        
        // TODO: Phase 3 - Implement OCR and LLM parsing
        // 1. Load file from storage
        // 2. Call OCR service to extract text
        // 3. Call LLM parser to extract structured data
        // 4. Update receipt with extracted data
        // 5. Update status to COMPLETED or FAILED
        
        log.warn("Receipt processing not yet implemented. Receipt ID: {}", receiptId);
        return receipt;
    }
}

