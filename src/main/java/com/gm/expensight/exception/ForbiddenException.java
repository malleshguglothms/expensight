package com.gm.expensight.exception;

public class ForbiddenException extends BaseException {
    
    private static final String ERROR_CODE = "FORBIDDEN";
    
    public ForbiddenException(String message) {
        super(message);
    }
    
    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}

