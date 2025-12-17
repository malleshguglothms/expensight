package com.gm.expensight.exception;

public class OcrException extends BaseException {
    
    private static final String ERROR_CODE = "OCR_PROCESSING_ERROR";
    
    public OcrException(String message) {
        super(message);
    }
    
    public OcrException(String message, Throwable cause) {
        super(message, cause);
    }
    
    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}

