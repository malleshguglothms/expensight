package com.gm.expensight.service;

import com.gm.expensight.domain.model.Receipt;
import com.gm.expensight.domain.model.ReceiptItem;
import com.gm.expensight.web.dto.ReceiptResponse;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ReceiptMapper {
    
    public ReceiptResponse toResponse(Receipt receipt) {
        if (receipt == null) {
            return null;
        }
        
        return ReceiptResponse.builder()
                .merchantName(receipt.getMerchantName())
                .totalAmount(receipt.getTotalAmount())
                .receiptDate(receipt.getReceiptDate())
                .taxAmount(receipt.getTaxAmount())
                .currency(receipt.getCurrency() != null ? receipt.getCurrency() : "INR")
                .items(receipt.getItems() != null ? 
                        receipt.getItems().stream()
                                .map(this::toItemResponse)
                                .collect(Collectors.toList()) : 
                        java.util.Collections.emptyList())
                .build();
    }
    
    private ReceiptResponse.ReceiptItemResponse toItemResponse(ReceiptItem item) {
        return ReceiptResponse.ReceiptItemResponse.builder()
                .itemName(item.getItemName())
                .quantity(item.getQuantity() != null ? item.getQuantity() : 1)
                .build();
    }
}

