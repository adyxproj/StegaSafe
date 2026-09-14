package com.stegasafe.dto;

public class CapacityInfo {
    private int width;
    private int height;
    private long totalPixels;
    private long rawCapacityBytes;
    private long maxMessageBytesUnencrypted;
    private long maxMessageBytesEncrypted;
    private String formattedMaxCapacity;
    private String filename;
    private long imageFileSizeBytes;
    private String formattedFileSize;

    public CapacityInfo() {
    }

    public CapacityInfo(int width, int height, long totalPixels, long rawCapacityBytes,
                        long maxMessageBytesUnencrypted, long maxMessageBytesEncrypted,
                        String formattedMaxCapacity, String filename,
                        long imageFileSizeBytes, String formattedFileSize) {
        this.width = width;
        this.height = height;
        this.totalPixels = totalPixels;
        this.rawCapacityBytes = rawCapacityBytes;
        this.maxMessageBytesUnencrypted = maxMessageBytesUnencrypted;
        this.maxMessageBytesEncrypted = maxMessageBytesEncrypted;
        this.formattedMaxCapacity = formattedMaxCapacity;
        this.filename = filename;
        this.imageFileSizeBytes = imageFileSizeBytes;
        this.formattedFileSize = formattedFileSize;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public long getTotalPixels() {
        return totalPixels;
    }

    public void setTotalPixels(long totalPixels) {
        this.totalPixels = totalPixels;
    }

    public long getRawCapacityBytes() {
        return rawCapacityBytes;
    }

    public void setRawCapacityBytes(long rawCapacityBytes) {
        this.rawCapacityBytes = rawCapacityBytes;
    }

    public long getMaxMessageBytesUnencrypted() {
        return maxMessageBytesUnencrypted;
    }

    public void setMaxMessageBytesUnencrypted(long maxMessageBytesUnencrypted) {
        this.maxMessageBytesUnencrypted = maxMessageBytesUnencrypted;
    }

    public long getMaxMessageBytesEncrypted() {
        return maxMessageBytesEncrypted;
    }

    public void setMaxMessageBytesEncrypted(long maxMessageBytesEncrypted) {
        this.maxMessageBytesEncrypted = maxMessageBytesEncrypted;
    }

    public String getFormattedMaxCapacity() {
        return formattedMaxCapacity;
    }

    public void setFormattedMaxCapacity(String formattedMaxCapacity) {
        this.formattedMaxCapacity = formattedMaxCapacity;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getImageFileSizeBytes() {
        return imageFileSizeBytes;
    }

    public void setImageFileSizeBytes(long imageFileSizeBytes) {
        this.imageFileSizeBytes = imageFileSizeBytes;
    }

    public String getFormattedFileSize() {
        return formattedFileSize;
    }

    public void setFormattedFileSize(String formattedFileSize) {
        this.formattedFileSize = formattedFileSize;
    }
}
