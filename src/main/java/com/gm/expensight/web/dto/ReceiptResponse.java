package com.gm.expensight.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptResponse {
    
    private String merchantName;
    private BigDecimal totalAmount;
    private LocalDate receiptDate;
    private BigDecimal taxAmount;
    private String currency;
    
    @Builder.Default
    private List<ReceiptItemResponse> items = new ArrayList<>();
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiptItemResponse {
        private String itemName;
        private Integer quantity;
    }
}

