package com.gm.expensight.service.impl;

import com.gm.expensight.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new LocalFileStorageService(tempDir);
    }

    @Test
    void shouldStoreFileSuccessfully() throws IOException {
        // Given
        String userEmail = "test@example.com";
        MultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // When
        String storagePath = fileStorageService.storeFile(file, userEmail);

        // Then
        assertNotNull(storagePath);
        assertTrue(Files.exists(tempDir.resolve(storagePath)));
        assertTrue(Files.isRegularFile(tempDir.resolve(storagePath)));
    }

    @Test
    void shouldCreateUserSpecificDirectory() throws IOException {
        // Given
        String userEmail = "user@example.com";
        MultipartFile file = new MockMultipartFile(
                "file",
                "receipt.png",
                "image/png",
                "content".getBytes()
        );

        // When
        String storagePath = fileStorageService.storeFile(file, userEmail);

        // Then
        assertTrue(storagePath.contains("user_at_example_com"));
        Path userDir = tempDir.resolve("user_at_example_com");
        assertTrue(Files.exists(userDir));
        assertTrue(Files.isDirectory(userDir));
    }

    @Test
    void shouldGenerateUniqueFileName() throws IOException {
        // Given
        String userEmail = "test@example.com";
        MultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpeg",
                "content".getBytes()
        );

        // When
        String path1 = fileStorageService.storeFile(file, userEmail);
        String path2 = fileStorageService.storeFile(file, userEmail);

        // Then
        assertNotEquals(path1, path2);
    }

    @Test
    void shouldHandleSpecialCharactersInFileName() throws IOException {
        // Given
        String userEmail = "test@example.com";
        MultipartFile file = new MockMultipartFile(
                "file",
                "receipt with spaces & special chars.jpg",
                "image/jpeg",
                "content".getBytes()
        );

        // When
        String storagePath = fileStorageService.storeFile(file, userEmail);

        // Then
        assertNotNull(storagePath);
        assertTrue(Files.exists(tempDir.resolve(storagePath)));
    }

    @Test
    void shouldThrowExceptionWhenFileIsNull() {
        // Given
        String userEmail = "test@example.com";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            fileStorageService.storeFile(null, userEmail);
        });
    }

    @Test
    void shouldThrowExceptionWhenUserEmailIsNull() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpeg",
                "content".getBytes()
        );

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            fileStorageService.storeFile(file, null);
        });
    }

    @Test
    void shouldPreserveFileContent() throws IOException {
        // Given
        String userEmail = "test@example.com";
        byte[] originalContent = "original file content".getBytes();
        MultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpeg",
                originalContent
        );

        // When
        String storagePath = fileStorageService.storeFile(file, userEmail);
        byte[] storedContent = Files.readAllBytes(tempDir.resolve(storagePath));

        // Then
        assertArrayEquals(originalContent, storedContent);
    }

    @Test
    void shouldLoadFileSuccessfully() throws IOException {
        // Given
        String userEmail = "test@example.com";
        byte[] originalContent = "test content".getBytes();
        MultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpeg",
                originalContent
        );
        String storagePath = fileStorageService.storeFile(file, userEmail);

        // When
        byte[] loadedContent = fileStorageService.loadFile(storagePath);

        // Then
        assertArrayEquals(originalContent, loadedContent);
    }

    @Test
    void shouldThrowExceptionWhenLoadingNonExistentFile() {
        // When & Then
        assertThrows(IOException.class, () -> {
            fileStorageService.loadFile("non/existent/path.jpg");
        });
    }

    @Test
    void shouldDeleteFileSuccessfully() throws IOException {
        // Given
        String userEmail = "test@example.com";
        MultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpeg",
                "content".getBytes()
        );
        String storagePath = fileStorageService.storeFile(file, userEmail);
        assertTrue(Files.exists(tempDir.resolve(storagePath)));

        // When
        fileStorageService.deleteFile(storagePath);

        // Then
        assertFalse(Files.exists(tempDir.resolve(storagePath)));
    }
}

