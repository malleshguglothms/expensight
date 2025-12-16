package com.gm.expensight.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadReceiptResponse {
    private UUID receiptId;
    private String fileName;
    private String status;
    private LocalDateTime uploadedAt;
    private String message;
}

