package com.gm.expensight.web;

import com.gm.expensight.domain.model.Receipt;
import com.gm.expensight.service.ReceiptService;
import com.gm.expensight.web.dto.UploadReceiptResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @PostMapping("/upload")
    public ResponseEntity<UploadReceiptResponse> uploadReceipt(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal OAuth2User principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userEmail = principal.getAttribute("email");
        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Receipt receipt = receiptService.uploadReceipt(file, userEmail);

            UploadReceiptResponse response = UploadReceiptResponse.builder()
                    .receiptId(receipt.getId())
                    .fileName(receipt.getFileMetadata().getFileName())
                    .status(receipt.getStatus().name())
                    .uploadedAt(receipt.getCreatedAt())
                    .message("Receipt uploaded successfully")
                    .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid file upload: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(UploadReceiptResponse.builder()
                            .message("Invalid file: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Failed to upload receipt: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(UploadReceiptResponse.builder()
                            .message("Failed to upload receipt: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping
    public ResponseEntity<List<Receipt>> getReceipts(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userEmail = principal.getAttribute("email");
        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Receipt> receipts = receiptService.getUserReceipts(userEmail);
        return ResponseEntity.ok(receipts);
    }

    @GetMapping("/{receiptId}")
    public ResponseEntity<Receipt> getReceipt(
            @PathVariable UUID receiptId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userEmail = principal.getAttribute("email");
        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Receipt receipt = receiptService.getReceiptById(receiptId);
            
            if (!receipt.getUserEmail().equals(userEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            return ResponseEntity.ok(receipt);
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

