package com.gm.expensight.service;

import com.gm.expensight.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Component
public class FileValidator {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            MediaType.IMAGE_JPEG_VALUE,
            "image/jpg",
            MediaType.IMAGE_PNG_VALUE,
            MediaType.APPLICATION_PDF_VALUE
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("File cannot be null or empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException(
                    String.format("File size (%d bytes) exceeds maximum allowed size of %d bytes (10MB)",
                            file.getSize(), MAX_FILE_SIZE));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ValidationException(
                    String.format("Invalid file type: %s. Allowed types: %s",
                            contentType, String.join(", ", ALLOWED_CONTENT_TYPES)));
        }
    }

    public long getMaxFileSize() {
        return MAX_FILE_SIZE;
    }

    public List<String> getAllowedContentTypes() {
        return List.copyOf(ALLOWED_CONTENT_TYPES);
    }
}

