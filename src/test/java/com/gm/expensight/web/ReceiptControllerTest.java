package com.gm.expensight.web;

import com.gm.expensight.domain.model.FileMetadata;
import com.gm.expensight.domain.model.ProcessingStatus;
import com.gm.expensight.domain.model.Receipt;
import com.gm.expensight.service.ReceiptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReceiptController.class)
@TestPropertySource(properties = "storage.location=test-upload-dir")
@SuppressWarnings("removal")
class ReceiptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReceiptService receiptService;

    @Test
    void shouldUploadReceiptSuccessfully() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        String storagePath = "test_at_example_com/receipt.jpg";
        Receipt savedReceipt = Receipt.builder()
                .id(UUID.randomUUID())
                .userEmail("test@example.com")
                .merchantName("Unknown")
                .totalAmount(BigDecimal.ZERO)
                .receiptDate(LocalDate.now())
                .status(ProcessingStatus.PENDING)
                .fileMetadata(FileMetadata.builder()
                        .id(UUID.randomUUID())
                        .fileName("receipt.jpg")
                        .contentType("image/jpeg")
                        .storagePath(storagePath)
                        .uploadedAt(LocalDateTime.now())
                        .build())
                .createdAt(LocalDateTime.now())
                .build();

        when(receiptService.uploadReceipt(any(), anyString())).thenReturn(savedReceipt);

        // When & Then
        mockMvc.perform(multipart("/receipts/upload")
                        .file(file)
                        .with(oauth2Login().attributes(attrs -> attrs.put("email", "test@example.com")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptId").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.fileName").value("receipt.jpg"))
                .andExpect(jsonPath("$.message").value("Receipt uploaded successfully"));
    }

    @Test
    void shouldRejectInvalidFileType() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "text content".getBytes()
        );

        when(receiptService.uploadReceipt(any(), anyString()))
                .thenThrow(new IllegalArgumentException("Invalid file type"));

        // When & Then
        mockMvc.perform(multipart("/receipts/upload")
                        .file(file)
                        .with(oauth2Login().attributes(attrs -> attrs.put("email", "test@example.com")))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldRejectEmptyFile() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[0]
        );

        when(receiptService.uploadReceipt(any(), anyString()))
                .thenThrow(new IllegalArgumentException("File cannot be null or empty"));

        // When & Then
        mockMvc.perform(multipart("/receipts/upload")
                        .file(file)
                        .with(oauth2Login().attributes(attrs -> attrs.put("email", "test@example.com")))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "content".getBytes()
        );

        // When & Then
        // Without authentication, Spring Security redirects to login (302) instead of 401
        mockMvc.perform(multipart("/receipts/upload")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection()); // Redirects to login page
    }
}

