package com.stegasafe.exception;

public class UnsupportedImageException extends SteganographyException {
    public UnsupportedImageException(String message) {
        super(message);
    }

    public UnsupportedImageException(String message, Throwable cause) {
        super(message, cause);
    }
}
