package com.gm.expensight.web;

import com.gm.expensight.domain.model.Receipt;
import com.gm.expensight.exception.ForbiddenException;
import com.gm.expensight.exception.UnauthorizedException;
import com.gm.expensight.service.ReceiptMapper;
import com.gm.expensight.service.ReceiptService;
import com.gm.expensight.web.dto.ReceiptResponse;
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
    private final ReceiptMapper receiptMapper;

    @PostMapping("/upload")
    public ResponseEntity<UploadReceiptResponse> uploadReceipt(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal OAuth2User principal) {

        validateAuthentication(principal);

        String userEmail = extractUserEmail(principal);
        Receipt receipt = receiptService.uploadReceipt(file, userEmail);

        UploadReceiptResponse response = UploadReceiptResponse.builder()
                .receiptId(receipt.getId())
                .fileName(receipt.getFileMetadata().getFileName())
                .status(receipt.getStatus().name())
                .uploadedAt(receipt.getCreatedAt())
                .message("Receipt uploaded successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ReceiptResponse>> getReceipts(@AuthenticationPrincipal OAuth2User principal) {
        validateAuthentication(principal);

        String userEmail = extractUserEmail(principal);
        List<Receipt> receipts = receiptService.getUserReceipts(userEmail);
        List<ReceiptResponse> responses = receipts.stream()
                .map(receiptMapper::toResponse)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{receiptId}")
    public ResponseEntity<ReceiptResponse> getReceipt(
            @PathVariable UUID receiptId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        validateAuthentication(principal);

        String userEmail = extractUserEmail(principal);
        Receipt receipt = receiptService.getReceiptById(receiptId);
        
        if (!receipt.getUserEmail().equals(userEmail)) {
            throw new ForbiddenException("Access denied: Receipt belongs to another user");
        }
        
        return ResponseEntity.ok(receiptMapper.toResponse(receipt));
    }
    
    private void validateAuthentication(OAuth2User principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
    }
    
    private String extractUserEmail(OAuth2User principal) {
        String email = principal.getAttribute("email");
        if (email == null) {
            throw new UnauthorizedException("User email not found in authentication");
        }
        return email;
    }
}

