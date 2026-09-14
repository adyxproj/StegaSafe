package com.stegasafe.exception;

public class CapacityExceededException extends SteganographyException {
    private final long requiredBytes;
    private final long availableBytes;

    public CapacityExceededException(String message, long requiredBytes, long availableBytes) {
        super(message);
        this.requiredBytes = requiredBytes;
        this.availableBytes = availableBytes;
    }

    public long getRequiredBytes() {
        return requiredBytes;
    }

    public long getAvailableBytes() {
        return availableBytes;
    }
}
