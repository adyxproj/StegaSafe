package com.stegasafe.dto;

public class DecodeResult {
    private boolean success;
    private String message;
    private boolean encrypted;
    private int payloadSizeBytes;
    private int characterCount;
    private long extractionTimeMs;

    public DecodeResult() {
    }

    public DecodeResult(boolean success, String message, boolean encrypted, int payloadSizeBytes, int characterCount, long extractionTimeMs) {
        this.success = success;
        this.message = message;
        this.encrypted = encrypted;
        this.payloadSizeBytes = payloadSizeBytes;
        this.characterCount = characterCount;
        this.extractionTimeMs = extractionTimeMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public int getPayloadSizeBytes() {
        return payloadSizeBytes;
    }

    public void setPayloadSizeBytes(int payloadSizeBytes) {
        this.payloadSizeBytes = payloadSizeBytes;
    }

    public int getCharacterCount() {
        return characterCount;
    }

    public void setCharacterCount(int characterCount) {
        this.characterCount = characterCount;
    }

    public long getExtractionTimeMs() {
        return extractionTimeMs;
    }

    public void setExtractionTimeMs(long extractionTimeMs) {
        this.extractionTimeMs = extractionTimeMs;
    }
}
