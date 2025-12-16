package com.gm.expensight.service;

import com.gm.expensight.domain.model.Receipt;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ReceiptService {
    
    Receipt uploadReceipt(MultipartFile file, String userEmail);
    
    List<Receipt> getUserReceipts(String userEmail);
    
    Receipt getReceiptById(UUID receiptId);
    
    Receipt processReceipt(UUID receiptId);
}

