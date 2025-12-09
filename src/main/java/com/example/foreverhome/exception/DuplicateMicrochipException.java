package com.example.foreverhome.exception;

public class DuplicateMicrochipException extends RuntimeException {
    public DuplicateMicrochipException(String microchipId) {
        super("Microchip already registered: " + microchipId);
    }
}
