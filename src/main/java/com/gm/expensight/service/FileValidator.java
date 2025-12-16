package com.gm.expensight.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Validates uploaded files according to business rules.
 * 
 * Follows Single Responsibility Principle - only handles file validation.
 * This allows validation logic to be reused across different entry points
 * (REST API, web forms, etc.)
 */
@Slf4j
@Component
public class FileValidator {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            MediaType.IMAGE_JPEG_VALUE,
            "image/jpg", // Some systems use jpg instead of jpeg
            MediaType.IMAGE_PNG_VALUE,
            MediaType.APPLICATION_PDF_VALUE
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * Validates a file according to business rules.
     * 
     * @param file the file to validate
     * @throws IllegalArgumentException if file is invalid
     */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("File size (%d bytes) exceeds maximum allowed size of %d bytes (10MB)",
                            file.getSize(), MAX_FILE_SIZE));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    String.format("Invalid file type: %s. Allowed types: %s",
                            contentType, String.join(", ", ALLOWED_CONTENT_TYPES)));
        }

        log.debug("File validation passed: name={}, size={}, type={}",
                file.getOriginalFilename(), file.getSize(), contentType);
    }

    /**
     * Gets the maximum allowed file size in bytes.
     * 
     * @return maximum file size in bytes
     */
    public long getMaxFileSize() {
        return MAX_FILE_SIZE;
    }

    /**
     * Gets the list of allowed content types.
     * 
     * @return list of allowed MIME types
     */
    public List<String> getAllowedContentTypes() {
        return List.copyOf(ALLOWED_CONTENT_TYPES);
    }
}

