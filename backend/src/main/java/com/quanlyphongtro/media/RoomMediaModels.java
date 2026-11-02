package com.quanlyphongtro.media;

import java.time.Instant;

public final class RoomMediaModels {

    private RoomMediaModels() {
    }

    public record RoomMediaResponse(
            long id,
            long roomId,
            String mediaType,
            String originalFileName,
            String storedFileName,
            String url,
            String mimeType,
            long sizeBytes,
            int displayOrder,
            boolean primaryImage,
            String description,
            Instant uploadedAt,
            Long uploadedBy
    ) {
    }

    public record OrderRequest(int displayOrder) {
    }

    public record DescriptionRequest(String description) {
    }
}
