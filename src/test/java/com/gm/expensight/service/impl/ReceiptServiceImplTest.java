package com.gm.expensight.service.impl;

import com.gm.expensight.domain.model.FileMetadata;
import com.gm.expensight.domain.model.ProcessingStatus;
import com.gm.expensight.domain.model.Receipt;
import com.gm.expensight.repository.ReceiptRepository;
import com.gm.expensight.service.FileStorageService;
import com.gm.expensight.service.FileValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceImplTest {

    @Mock
    private FileValidator fileValidator;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ReceiptRepository receiptRepository;

    @InjectMocks
    private ReceiptServiceImpl receiptService;

    private MultipartFile mockFile;
    private String userEmail;
    private String storagePath;

    @BeforeEach
    void setUp() {
        userEmail = "test@example.com";
        storagePath = "test_at_example_com/receipt.jpg";
        mockFile = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpeg",
                "test content".getBytes()
        );
    }

    @Test
    void shouldUploadReceiptSuccessfully() {
        // Given
        when(fileStorageService.storeFile(any(MultipartFile.class), anyString()))
                .thenReturn(storagePath);

        Receipt savedReceipt = Receipt.builder()
                .id(UUID.randomUUID())
                .userEmail(userEmail)
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

        when(receiptRepository.save(any(Receipt.class))).thenReturn(savedReceipt);

        // When
        Receipt result = receiptService.uploadReceipt(mockFile, userEmail);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserEmail()).isEqualTo(userEmail);
        assertThat(result.getStatus()).isEqualTo(ProcessingStatus.PENDING);
        assertThat(result.getFileMetadata()).isNotNull();
        assertThat(result.getFileMetadata().getFileName()).isEqualTo("receipt.jpg");

        verify(fileValidator).validate(mockFile);
        verify(fileStorageService).storeFile(mockFile, userEmail);
        verify(receiptRepository).save(any(Receipt.class));
    }

    @Test
    void shouldThrowExceptionWhenFileValidationFails() {
        // Given
        doThrow(new IllegalArgumentException("Invalid file"))
                .when(fileValidator).validate(mockFile);

        // When & Then
        assertThatThrownBy(() -> receiptService.uploadReceipt(mockFile, userEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid file");

        verify(fileValidator).validate(mockFile);
        verify(fileStorageService, never()).storeFile(any(), anyString());
        verify(receiptRepository, never()).save(any(Receipt.class));
    }

    @Test
    void shouldThrowExceptionWhenFileStorageFails() {
        // Given
        when(fileStorageService.storeFile(any(MultipartFile.class), anyString()))
                .thenThrow(new RuntimeException("Storage failed"));

        // When & Then
        assertThatThrownBy(() -> receiptService.uploadReceipt(mockFile, userEmail))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Storage failed");

        verify(fileValidator).validate(mockFile);
        verify(fileStorageService).storeFile(mockFile, userEmail);
        verify(receiptRepository, never()).save(any(Receipt.class));
    }

    @Test
    void shouldGetUserReceiptsSuccessfully() {
        // Given
        Receipt receipt1 = createReceipt(UUID.randomUUID(), userEmail);
        Receipt receipt2 = createReceipt(UUID.randomUUID(), userEmail);
        List<Receipt> expectedReceipts = Arrays.asList(receipt1, receipt2);

        when(receiptRepository.findByUserEmailOrderByCreatedAtDesc(userEmail))
                .thenReturn(expectedReceipts);

        // When
        List<Receipt> result = receiptService.getUserReceipts(userEmail);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(receipt1, receipt2);

        verify(receiptRepository).findByUserEmailOrderByCreatedAtDesc(userEmail);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoReceipts() {
        // Given
        when(receiptRepository.findByUserEmailOrderByCreatedAtDesc(userEmail))
                .thenReturn(List.of());

        // When
        List<Receipt> result = receiptService.getUserReceipts(userEmail);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(receiptRepository).findByUserEmailOrderByCreatedAtDesc(userEmail);
    }

    @Test
    void shouldGetReceiptByIdSuccessfully() {
        // Given
        UUID receiptId = UUID.randomUUID();
        Receipt expectedReceipt = createReceipt(receiptId, userEmail);

        when(receiptRepository.findById(receiptId))
                .thenReturn(Optional.of(expectedReceipt));

        // When
        Receipt result = receiptService.getReceiptById(receiptId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(receiptId);
        assertThat(result.getUserEmail()).isEqualTo(userEmail);

        verify(receiptRepository).findById(receiptId);
    }

    @Test
    void shouldThrowExceptionWhenReceiptNotFound() {
        // Given
        UUID receiptId = UUID.randomUUID();
        when(receiptRepository.findById(receiptId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> receiptService.getReceiptById(receiptId))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("Receipt not found with ID: " + receiptId);

        verify(receiptRepository).findById(receiptId);
    }

    @Test
    void shouldProcessReceiptSuccessfully() {
        // Given
        UUID receiptId = UUID.randomUUID();
        Receipt receipt = createReceipt(receiptId, userEmail);

        when(receiptRepository.findById(receiptId))
                .thenReturn(Optional.of(receipt));

        // When
        Receipt result = receiptService.processReceipt(receiptId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(receiptId);
        // Note: Processing logic is not yet implemented, so receipt is returned as-is

        verify(receiptRepository).findById(receiptId);
    }

    @Test
    void shouldThrowExceptionWhenProcessingNonExistentReceipt() {
        // Given
        UUID receiptId = UUID.randomUUID();
        when(receiptRepository.findById(receiptId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> receiptService.processReceipt(receiptId))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);

        verify(receiptRepository).findById(receiptId);
    }

    @Test
    void shouldCreateReceiptWithCorrectMetadata() {
        // Given
        when(fileStorageService.storeFile(any(MultipartFile.class), anyString()))
                .thenReturn(storagePath);

        Receipt savedReceipt = Receipt.builder()
                .id(UUID.randomUUID())
                .userEmail(userEmail)
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

        when(receiptRepository.save(any(Receipt.class))).thenReturn(savedReceipt);

        // When
        Receipt result = receiptService.uploadReceipt(mockFile, userEmail);

        // Then
        assertThat(result.getFileMetadata()).isNotNull();
        assertThat(result.getFileMetadata().getFileName()).isEqualTo("receipt.jpg");
        assertThat(result.getFileMetadata().getContentType()).isEqualTo("image/jpeg");
        assertThat(result.getFileMetadata().getStoragePath()).isEqualTo(storagePath);
        assertThat(result.getMerchantName()).isEqualTo("Unknown");
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getStatus()).isEqualTo(ProcessingStatus.PENDING);
    }

    private Receipt createReceipt(UUID id, String email) {
        return Receipt.builder()
                .id(id)
                .userEmail(email)
                .merchantName("Test Merchant")
                .totalAmount(BigDecimal.valueOf(100.00))
                .receiptDate(LocalDate.now())
                .status(ProcessingStatus.PENDING)
                .fileMetadata(FileMetadata.builder()
                        .id(UUID.randomUUID())
                        .fileName("test.jpg")
                        .contentType("image/jpeg")
                        .storagePath("path/to/file.jpg")
                        .uploadedAt(LocalDateTime.now())
                        .build())
                .createdAt(LocalDateTime.now())
                .build();
    }
}

