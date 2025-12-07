package com.example.foreverhome.exception;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(String message) {
        super(message);
    }

    public InvalidStatusTransitionException(String currentStatus, String targetStatus) {
        super("Cannot transition from " + currentStatus + " to " + targetStatus);
    }
}
