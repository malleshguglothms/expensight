package com.gm.expensight.exception;

public class ResourceNotFoundException extends BaseException {
    
    private static final String ERROR_CODE = "RESOURCE_NOT_FOUND";
    
    public ResourceNotFoundException(String resourceType, Object identifier) {
        super(String.format("%s not found with identifier: %s", resourceType, identifier));
    }
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}

