package com.gm.expensight.exception;

public class UnauthorizedException extends BaseException {
    
    private static final String ERROR_CODE = "UNAUTHORIZED";
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}

