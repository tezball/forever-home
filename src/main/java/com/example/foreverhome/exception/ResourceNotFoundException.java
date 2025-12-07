package com.example.foreverhome.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceType, Object identifier) {
        super(resourceType + " not found with identifier: " + identifier);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
