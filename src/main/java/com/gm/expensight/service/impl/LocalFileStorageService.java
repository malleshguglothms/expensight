package com.gm.expensight.service.impl;

import com.gm.expensight.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Local filesystem implementation of FileStorageService.
 * 
 * This implementation stores files on the local filesystem,
 * organized by user email in separate directories.
 * 
 * Follows Single Responsibility Principle - only handles file storage.
 */
@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    private Path storageLocation;

    public LocalFileStorageService() {
        // Default constructor for Spring
    }

    @Value("${storage.location:upload-dir}")
    public void setStorageLocation(String storageLocationPath) {
        this.storageLocation = Paths.get(storageLocationPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory", e);
        }
    }

    // Package-private constructor for testing (accessible from test package)
    LocalFileStorageService(Path storageLocation) {
        this.storageLocation = storageLocation.toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory", e);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String userEmail) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("User email cannot be null or empty");
        }

        try {
            Path userDir = this.storageLocation.resolve(sanitizeEmail(userEmail));
            Files.createDirectories(userDir);

            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = generateUniqueFilename(fileExtension);
            
            Path targetLocation = userDir.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return sanitizeEmail(userEmail) + "/" + uniqueFilename;
        } catch (IOException e) {
            log.error("Failed to store file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Override
    public byte[] loadFile(String storagePath) throws IOException {
        Path filePath = this.storageLocation.resolve(storagePath).normalize();
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + storagePath);
        }
        return Files.readAllBytes(filePath);
    }

    @Override
    public void deleteFile(String storagePath) throws IOException {
        Path filePath = this.storageLocation.resolve(storagePath).normalize();
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
    }

    private String sanitizeEmail(String email) {
        return email.replace("@", "_at_").replace(".", "_");
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private String generateUniqueFilename(String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s_%s%s", timestamp, uuid, extension);
    }
}

