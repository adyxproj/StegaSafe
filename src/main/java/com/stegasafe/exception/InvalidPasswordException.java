package com.stegasafe.exception;

public class InvalidPasswordException extends SteganographyException {
    public InvalidPasswordException(String message) {
        super(message);
    }

    public InvalidPasswordException(String message, Throwable cause) {
        super(message, cause);
    }
}
