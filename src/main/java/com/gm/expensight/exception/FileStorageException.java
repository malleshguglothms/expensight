package com.gm.expensight.exception;

public class FileStorageException extends BaseException {
    
    private static final String ERROR_CODE = "FILE_STORAGE_ERROR";
    
    public FileStorageException(String message) {
        super(message);
    }
    
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
    
    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}

