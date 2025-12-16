package com.gm.expensight.exception;

public class LlmException extends BaseException {
    
    private static final String ERROR_CODE = "LLM_PROCESSING_ERROR";
    
    public LlmException(String message) {
        super(message);
    }
    
    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
    
    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}

