package com.quanlyphongtro.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "app.media")
public class RoomMediaProperties {

    private Path storageDir = Path.of("uploads");
    private long maxImageSizeBytes = 20L * 1024 * 1024;
    private long maxVideoSizeBytes = 500L * 1024 * 1024;
    private long maxRequestSizeBytes = 1024L * 1024 * 1024;
    private int maxFilesPerUpload = 10;

    public Path getStorageDir() {
        return storageDir;
    }

    public void setStorageDir(Path storageDir) {
        this.storageDir = storageDir;
    }

    public long getMaxImageSizeBytes() {
        return maxImageSizeBytes;
    }

    public void setMaxImageSizeBytes(long maxImageSizeBytes) {
        this.maxImageSizeBytes = maxImageSizeBytes;
    }

    public long getMaxVideoSizeBytes() {
        return maxVideoSizeBytes;
    }

    public void setMaxVideoSizeBytes(long maxVideoSizeBytes) {
        this.maxVideoSizeBytes = maxVideoSizeBytes;
    }

    public int getMaxFilesPerUpload() {
        return maxFilesPerUpload;
    }

    public void setMaxFilesPerUpload(int maxFilesPerUpload) {
        this.maxFilesPerUpload = maxFilesPerUpload;
    }

    public long getMaxRequestSizeBytes() {
        return maxRequestSizeBytes;
    }

    public void setMaxRequestSizeBytes(long maxRequestSizeBytes) {
        this.maxRequestSizeBytes = maxRequestSizeBytes;
    }
}
