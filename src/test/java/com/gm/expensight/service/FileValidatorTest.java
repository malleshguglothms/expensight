package com.gm.expensight.service;

import com.gm.expensight.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class FileValidatorTest {

    @InjectMocks
    private FileValidator fileValidator;

    private MultipartFile validFile;

    @BeforeEach
    void setUp() {
        validFile = new MockMultipartFile(
                "file",
                "receipt.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test content".getBytes()
        );
    }

    @Test
    void shouldValidateValidJpegFile() {
        // When & Then - should not throw exception
        fileValidator.validate(validFile);
    }

    @Test
    void shouldValidateValidPngFile() {
        // Given
        MultipartFile pngFile = new MockMultipartFile(
                "file",
                "receipt.png",
                MediaType.IMAGE_PNG_VALUE,
                "test content".getBytes()
        );

        // When & Then - should not throw exception
        fileValidator.validate(pngFile);
    }

    @Test
    void shouldValidateValidPdfFile() {
        // Given
        MultipartFile pdfFile = new MockMultipartFile(
                "file",
                "receipt.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "test content".getBytes()
        );

        // When & Then - should not throw exception
        fileValidator.validate(pdfFile);
    }

    @Test
    void shouldValidateValidJpgFile() {
        // Given
        MultipartFile jpgFile = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpg", // lowercase jpg
                "test content".getBytes()
        );

        // When & Then - should not throw exception
        fileValidator.validate(jpgFile);
    }

    @Test
    void shouldThrowExceptionWhenFileIsNull() {
        // When & Then
        assertThatThrownBy(() -> fileValidator.validate(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("File cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionWhenFileIsEmpty() {
        // Given
        MultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[0]
        );

        // When & Then
        assertThatThrownBy(() -> fileValidator.validate(emptyFile))
                .isInstanceOf(ValidationException.class)
                .hasMessage("File cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionWhenFileSizeExceedsMaximum() {
        // Given
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        MultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                largeContent
        );

        // When & Then
        assertThatThrownBy(() -> fileValidator.validate(largeFile))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("exceeds maximum allowed size");
    }

    @Test
    void shouldThrowExceptionWhenFileSizeIsExactlyMaximum() {
        // Given
        byte[] maxSizeContent = new byte[10 * 1024 * 1024]; // Exactly 10MB
        MultipartFile maxSizeFile = new MockMultipartFile(
                "file",
                "max.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                maxSizeContent
        );

        // When & Then - should not throw (boundary test)
        fileValidator.validate(maxSizeFile);
    }

    @Test
    void shouldThrowExceptionWhenContentTypeIsNull() {
        // Given
        MultipartFile fileWithNullType = new MockMultipartFile(
                "file",
                "receipt.jpg",
                null,
                "test content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> fileValidator.validate(fileWithNullType))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid file type");
    }

    @Test
    void shouldThrowExceptionWhenContentTypeIsInvalid() {
        // Given
        MultipartFile invalidFile = new MockMultipartFile(
                "file",
                "document.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "text content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> fileValidator.validate(invalidFile))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid file type");
    }

    @Test
    void shouldThrowExceptionWhenContentTypeIsCaseInsensitive() {
        // Given
        MultipartFile uppercaseFile = new MockMultipartFile(
                "file",
                "receipt.JPG",
                "IMAGE/JPEG", // uppercase
                "test content".getBytes()
        );

        // When & Then - should still validate (case insensitive)
        fileValidator.validate(uppercaseFile);
    }

    @Test
    void shouldGetMaxFileSize() {
        // When
        long maxSize = fileValidator.getMaxFileSize();

        // Then
        assertThat(maxSize).isEqualTo(10 * 1024 * 1024); // 10MB
    }

    @Test
    void shouldGetAllowedContentTypes() {
        // When
        List<String> allowedTypes = fileValidator.getAllowedContentTypes();

        // Then
        assertThat(allowedTypes).isNotNull();
        assertThat(allowedTypes).contains(
                MediaType.IMAGE_JPEG_VALUE,
                "image/jpg",
                MediaType.IMAGE_PNG_VALUE,
                MediaType.APPLICATION_PDF_VALUE
        );
        assertThat(allowedTypes).hasSize(4);
    }

    @Test
    void shouldValidateFileWithSpecialCharactersInName() {
        // Given
        MultipartFile specialNameFile = new MockMultipartFile(
                "file",
                "receipt with spaces & special chars.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test content".getBytes()
        );

        // When & Then - should not throw exception
        fileValidator.validate(specialNameFile);
    }

    @Test
    void shouldValidateFileAtMaxAllowedSize() {
        // Given
        byte[] content = new byte[10 * 1024 * 1024 - 1]; // 1 byte less than max
        MultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                content
        );

        // When & Then - should not throw exception
        fileValidator.validate(file);
    }
}

