package com.gm.expensight.service;

/**
 * Exception thrown when OCR processing fails.
 */
public class OcrException extends Exception {
    
    public OcrException(String message) {
        super(message);
    }
    
    public OcrException(String message, Throwable cause) {
        super(message, cause);
    }
}
