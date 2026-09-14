package com.stegasafe.exception;

public class CorruptedPayloadException extends SteganographyException {
    public CorruptedPayloadException(String message) {
        super(message);
    }

    public CorruptedPayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
