package com.quanlyphongtro.media;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

@Component
public class LocalRoomMediaStorage implements RoomMediaStorage {

    private final Path storageRoot;

    public LocalRoomMediaStorage(RoomMediaProperties properties) {
        this.storageRoot = properties.getStorageDir().toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(long roomId, String mediaType, String extension, UUID fileId, MultipartFile file)
            throws IOException {
        String category = "IMAGE".equals(mediaType) ? "images" : "videos";
        String fileName = fileId + "." + extension.toLowerCase(Locale.ROOT);
        Path directory = storageRoot.resolve("rooms").resolve(Long.toString(roomId)).resolve(category).normalize();
        Path target = directory.resolve(fileName).normalize();
        if (!directory.startsWith(storageRoot) || !target.startsWith(directory)) {
            throw new IOException("Resolved upload path is outside the configured storage directory.");
        }

        Files.createDirectories(directory);
        try {
            file.transferTo(target);
            return new StoredFile(
                    fileName,
                    "/api/v1/media-files/rooms/" + roomId + "/" + category + "/" + fileName,
                    target
            );
        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(target);
            throw exception;
        }
    }

    @Override
    public void delete(StoredFile file) throws IOException {
        Path target = file.absolutePath().toAbsolutePath().normalize();
        if (!target.startsWith(storageRoot)) {
            throw new IOException("Refusing to delete a file outside the configured storage directory.");
        }
        Files.deleteIfExists(target);
    }

    @Override
    public Path getStorageRoot() {
        return storageRoot;
    }
}
