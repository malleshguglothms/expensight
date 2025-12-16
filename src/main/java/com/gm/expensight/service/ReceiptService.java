package com.gm.expensight.service;

import com.gm.expensight.domain.model.Receipt;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for receipt-related business operations.
 * 
 * Follows Dependency Inversion Principle - controllers depend on abstraction.
 * This allows for easy testing and future enhancements.
 */
public interface ReceiptService {
    
    /**
     * Uploads a receipt file and creates a receipt entity.
     * 
     * This method orchestrates:
     * 1. File validation
     * 2. File storage
     * 3. Receipt entity creation
     * 
     * @param file the receipt file to upload
     * @param userEmail the email of the user uploading the receipt
     * @return the created receipt entity
     * @throws IllegalArgumentException if file is invalid
     * @throws RuntimeException if upload or storage fails
     */
    Receipt uploadReceipt(MultipartFile file, String userEmail);
    
    /**
     * Retrieves all receipts for a given user.
     * 
     * @param userEmail the email of the user
     * @return list of receipts ordered by creation date (newest first)
     */
    List<Receipt> getUserReceipts(String userEmail);
    
    /**
     * Retrieves a receipt by ID.
     * 
     * @param receiptId the receipt ID
     * @return the receipt if found
     * @throws jakarta.persistence.EntityNotFoundException if receipt not found
     */
    Receipt getReceiptById(UUID receiptId);
    
    /**
     * Processes a receipt (OCR + parsing).
     * This will be implemented in Phase 3 (AI Integration).
     * 
     * @param receiptId the receipt ID to process
     * @return the processed receipt with extracted data
     */
    Receipt processReceipt(UUID receiptId);
}

