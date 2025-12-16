package com.gm.expensight.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Interface for file storage operations.
 * Follows Dependency Inversion Principle - controllers depend on abstraction.
 * 
 * Implementations can support different storage backends:
 * - Local filesystem
 * - AWS S3
 * - Azure Blob Storage
 * - Google Cloud Storage
 */
public interface FileStorageService {
    
    /**
     * Stores a file and returns the storage path.
     * 
     * @param file the file to store
     * @param userEmail the email of the user uploading the file
     * @return the storage path (relative to storage root)
     * @throws IllegalArgumentException if file or userEmail is invalid
     * @throws RuntimeException if storage operation fails
     */
    String storeFile(MultipartFile file, String userEmail);
    
    /**
     * Loads a file from storage.
     * 
     * @param storagePath the storage path returned by storeFile
     * @return the file content as bytes
     * @throws java.io.IOException if file cannot be read
     */
    byte[] loadFile(String storagePath) throws java.io.IOException;
    
    /**
     * Deletes a file from storage.
     * 
     * @param storagePath the storage path to delete
     * @throws java.io.IOException if file cannot be deleted
     */
    void deleteFile(String storagePath) throws java.io.IOException;
}
