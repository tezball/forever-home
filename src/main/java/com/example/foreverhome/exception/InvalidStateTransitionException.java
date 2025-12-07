package com.example.foreverhome.exception;

/**
 * Exception thrown when an invalid state transition is attempted.
 */
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }

    public InvalidStateTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
