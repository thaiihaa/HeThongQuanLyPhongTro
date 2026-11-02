package com.quanlyphongtro.media;

import com.quanlyphongtro.auth.AuthException;
import com.quanlyphongtro.auth.AuthModels.AuthenticatedUser;
import com.quanlyphongtro.media.RoomMediaModels.DescriptionRequest;
import com.quanlyphongtro.media.RoomMediaModels.OrderRequest;
import com.quanlyphongtro.media.RoomMediaModels.RoomMediaResponse;
import com.quanlyphongtro.media.RoomMediaStorage.StoredFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class RoomMediaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoomMediaService.class);
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private final RoomMediaRepository repository;
    private final RoomMediaStorage storage;
    private final RoomMediaProperties properties;

    public RoomMediaService(
            RoomMediaRepository repository,
            RoomMediaStorage storage,
            RoomMediaProperties properties
    ) {
        this.repository = repository;
        this.storage = storage;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<RoomMediaResponse> list(long roomId, AuthenticatedUser user) {
        ensureViewAccess(roomId, user);
        return repository.list(roomId);
    }

    @Transactional
    public List<RoomMediaResponse> upload(
            long roomId, List<MultipartFile> files, AuthenticatedUser admin
    ) {
        requireAdmin(admin);
        ensureRoomExists(roomId);
        if (files == null || files.isEmpty()) {
            throw badRequest("EMPTY_UPLOAD", "Vui lòng chọn ít nhất một ảnh hoặc video.");
        }
        if (files.size() > properties.getMaxFilesPerUpload()) {
            throw badRequest("TOO_MANY_FILES", "Mỗi lần chỉ được tải tối đa "
                    + properties.getMaxFilesPerUpload() + " tệp.");
        }

        long requestBytes = 0;
        for (MultipartFile file : files) {
            if (file.getSize() > properties.getMaxRequestSizeBytes() - requestBytes) {
                throw badRequest("REQUEST_TOO_LARGE", "Tổng dung lượng tệp vượt quá giới hạn mỗi lần tải.");
            }
            requestBytes += file.getSize();
        }
        if (requestBytes > properties.getMaxRequestSizeBytes()) {
            throw badRequest("REQUEST_TOO_LARGE", "Tổng dung lượng tệp vượt quá giới hạn mỗi lần tải.");
        }

        List<StoredFile> storedFiles = new ArrayList<>();
        List<RoomMediaResponse> uploaded = new ArrayList<>();
        registerRollbackCleanup(storedFiles);

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw badRequest("EMPTY_FILE", "Không thể tải tệp rỗng.");
            }
            FileType fileType = inspect(file);
            long maxSize = fileType.mediaType().equals("IMAGE")
                    ? properties.getMaxImageSizeBytes() : properties.getMaxVideoSizeBytes();
            if (file.getSize() > maxSize) {
                throw badRequest("FILE_TOO_LARGE", "Tệp " + safeOriginalName(file) + " vượt quá giới hạn "
                        + formatMegabytes(maxSize) + " MB.");
            }

            UUID fileId = UUID.randomUUID();
            try {
                StoredFile storedFile = storage.store(roomId, fileType.mediaType(), fileType.extension(), fileId, file);
                storedFiles.add(storedFile);
                uploaded.add(repository.insert(
                        roomId,
                        fileType.mediaType(),
                        safeOriginalName(file),
                        storedFile,
                        fileType.mimeType(),
                        file.getSize(),
                        admin.id()
                ));
            } catch (IOException exception) {
                throw new AuthException(HttpStatus.INTERNAL_SERVER_ERROR, "MEDIA_STORAGE_FAILED",
                        "Không thể lưu tệp trên máy chủ.", exception);
            }
        }
        return List.copyOf(uploaded);
    }

    @Transactional
    public void delete(long roomId, long mediaId, AuthenticatedUser admin) {
        requireAdmin(admin);
        RoomMediaResponse existing = repository.find(roomId, mediaId)
                .orElseThrow(() -> notFound("Không tìm thấy tệp media của phòng."));
        if (!repository.delete(roomId, mediaId)) {
            throw notFound("Tệp media đã được xóa hoặc không tồn tại.");
        }
        StoredFile file = storedFile(roomId, existing);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    storage.delete(file);
                } catch (IOException exception) {
                    LOGGER.error("Database row {} was deleted but media file cleanup failed.", mediaId, exception);
                }
            }
        });
    }

    @Transactional
    public void setPrimary(long roomId, long mediaId, AuthenticatedUser admin) {
        requireAdmin(admin);
        ensureRoomExists(roomId);
        if (!repository.setPrimaryImage(roomId, mediaId)) {
            throw badRequest("INVALID_PRIMARY_MEDIA", "Chỉ có thể chọn ảnh của chính phòng này làm ảnh đại diện.");
        }
    }

    @Transactional
    public void updateOrder(long roomId, long mediaId, OrderRequest request, AuthenticatedUser admin) {
        requireAdmin(admin);
        if (request.displayOrder() < 0) {
            throw badRequest("INVALID_DISPLAY_ORDER", "Thứ tự hiển thị phải lớn hơn hoặc bằng 0.");
        }
        if (!repository.updateOrder(roomId, mediaId, request.displayOrder())) {
            throw notFound("Không tìm thấy tệp media của phòng.");
        }
    }

    @Transactional
    public void updateDescription(
            long roomId, long mediaId, DescriptionRequest request, AuthenticatedUser admin
    ) {
        requireAdmin(admin);
        String description = request.description();
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw badRequest("DESCRIPTION_TOO_LONG", "Mô tả không được vượt quá 500 ký tự.");
        }
        if (!repository.updateDescription(
                roomId, mediaId, StringUtils.hasText(description) ? description.trim() : null)) {
            throw notFound("Không tìm thấy tệp media của phòng.");
        }
    }

    private void ensureViewAccess(long roomId, AuthenticatedUser user) {
        ensureRoomExists(roomId);
        if (user == null) {
            if (!repository.isRoomPubliclyAvailable(roomId)) {
                throw new AuthException(HttpStatus.FORBIDDEN, "ROOM_MEDIA_FORBIDDEN",
                        "Bạn không được phép xem media của phòng này.");
            }
            return;
        }
        if ("ADMIN".equals(user.role())) {
            return;
        }
        if ("NGUOI_THUE".equals(user.role())
                && (repository.isRoomPubliclyAvailable(roomId)
                || repository.tenantCanViewRoom(roomId, user.id()))) {
            return;
        }
        throw new AuthException(HttpStatus.FORBIDDEN, "ROOM_MEDIA_FORBIDDEN",
                "Bạn không được phép xem media của phòng này.");
    }

    private void ensureRoomExists(long roomId) {
        if (roomId < 1 || !repository.roomExists(roomId)) {
            throw notFound("Không tìm thấy phòng trọ.");
        }
    }

    private void requireAdmin(AuthenticatedUser user) {
        if (user == null || !"ADMIN".equals(user.role())) {
            throw new AuthException(HttpStatus.FORBIDDEN, "ADMIN_REQUIRED",
                    "Chỉ ADMIN mới được quản lý media phòng trọ.");
        }
    }

    private FileType inspect(MultipartFile file) {
        String originalName = safeOriginalName(file);
        int dot = originalName.lastIndexOf('.');
        if (dot < 1 || dot == originalName.length() - 1) {
            throw badRequest("UNSUPPORTED_MEDIA_TYPE", "Tệp phải có phần mở rộng ảnh hoặc video được hỗ trợ.");
        }
        String extension = originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
        FileType expected = switch (extension) {
            case "jpg", "jpeg" -> new FileType("IMAGE", extension, "image/jpeg");
            case "png" -> new FileType("IMAGE", extension, "image/png");
            case "webp" -> new FileType("IMAGE", extension, "image/webp");
            case "mp4" -> new FileType("VIDEO", extension, "video/mp4");
            case "webm" -> new FileType("VIDEO", extension, "video/webm");
            default -> throw badRequest("UNSUPPORTED_MEDIA_TYPE", "Chỉ hỗ trợ JPG, JPEG, PNG, WEBP, MP4 và WEBM.");
        };
        if (!expected.mimeType().equalsIgnoreCase(file.getContentType())) {
            throw badRequest("MIME_TYPE_MISMATCH", "Loại MIME của tệp không khớp với phần mở rộng.");
        }

        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            boolean valid = switch (expected.mimeType()) {
                case "image/jpeg" -> header.length >= 3
                        && unsigned(header[0]) == 0xff && unsigned(header[1]) == 0xd8 && unsigned(header[2]) == 0xff;
                case "image/png" -> startsWith(header, new byte[]{
                        (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
                });
                case "image/webp" -> header.length >= 12
                        && ascii(header, 0, 4).equals("RIFF") && ascii(header, 8, 4).equals("WEBP");
                case "video/mp4" -> header.length >= 8 && ascii(header, 4, 4).equals("ftyp");
                case "video/webm" -> startsWith(header, new byte[]{0x1a, 0x45, (byte) 0xdf, (byte) 0xa3});
                default -> false;
            };
            if (!valid) {
                throw badRequest("INVALID_FILE_CONTENT", "Nội dung tệp không đúng định dạng được khai báo.");
            }
        } catch (IOException exception) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "FILE_READ_FAILED",
                    "Không thể đọc nội dung tệp đã chọn.", exception);
        }
        return expected;
    }

    private String safeOriginalName(MultipartFile file) {
        String supplied = file.getOriginalFilename();
        if (!StringUtils.hasText(supplied)) {
            throw badRequest("INVALID_FILE_NAME", "Tên tệp không hợp lệ.");
        }
        String leafName = supplied.replace('\\', '/');
        leafName = leafName.substring(leafName.lastIndexOf('/') + 1).trim();
        if (leafName.isEmpty() || leafName.length() > 255 || leafName.chars().anyMatch(Character::isISOControl)) {
            throw badRequest("INVALID_FILE_NAME", "Tên tệp không hợp lệ hoặc quá dài.");
        }
        return leafName;
    }

    private void registerRollbackCleanup(List<StoredFile> storedFiles) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_ROLLED_BACK) {
                    return;
                }
                for (StoredFile file : storedFiles) {
                    try {
                        storage.delete(file);
                    } catch (IOException exception) {
                        LOGGER.error("Failed to remove media file after database rollback: {}",
                                file.absolutePath(), exception);
                    }
                }
            }
        });
    }

    private StoredFile storedFile(long roomId, RoomMediaResponse media) {
        String category = "IMAGE".equals(media.mediaType()) ? "images" : "videos";
        Path absolutePath = storage.getStorageRoot()
                .resolve("rooms").resolve(Long.toString(roomId)).resolve(category)
                .resolve(media.storedFileName()).normalize();
        return new StoredFile(media.storedFileName(), media.url(), absolutePath);
    }

    private AuthException badRequest(String code, String message) {
        return new AuthException(HttpStatus.BAD_REQUEST, code, message);
    }

    private AuthException notFound(String message) {
        return new AuthException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    private int unsigned(byte value) {
        return value & 0xff;
    }

    private boolean startsWith(byte[] input, byte[] prefix) {
        if (input.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (input[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private String ascii(byte[] input, int offset, int length) {
        return new String(input, offset, length, StandardCharsets.US_ASCII);
    }

    private String formatMegabytes(long bytes) {
        return Long.toString(bytes / (1024 * 1024));
    }

    private record FileType(String mediaType, String extension, String mimeType) {
    }
}
