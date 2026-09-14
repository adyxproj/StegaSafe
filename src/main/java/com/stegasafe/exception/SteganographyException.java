package com.stegasafe.exception;

public class SteganographyException extends RuntimeException {
    public SteganographyException(String message) {
        super(message);
    }

    public SteganographyException(String message, Throwable cause) {
        super(message, cause);
    }
}
