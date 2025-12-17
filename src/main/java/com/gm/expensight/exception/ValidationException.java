package com.gm.expensight.exception;

public class ValidationException extends BaseException {
    
    private static final String ERROR_CODE = "VALIDATION_ERROR";
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}

