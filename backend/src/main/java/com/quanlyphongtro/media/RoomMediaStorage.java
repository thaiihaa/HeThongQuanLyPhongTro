package com.quanlyphongtro.media;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public interface RoomMediaStorage {

    StoredFile store(long roomId, String mediaType, String extension, UUID fileId, MultipartFile file) throws IOException;

    void delete(StoredFile file) throws IOException;

    Path getStorageRoot();

    record StoredFile(String fileName, String url, Path absolutePath) {
    }
}
