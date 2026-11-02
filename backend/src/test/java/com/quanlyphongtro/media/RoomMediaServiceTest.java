package com.quanlyphongtro.media;

import com.quanlyphongtro.auth.AuthException;
import com.quanlyphongtro.auth.AuthModels.AuthenticatedUser;
import com.quanlyphongtro.media.RoomMediaModels.RoomMediaResponse;
import com.quanlyphongtro.media.RoomMediaStorage.StoredFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomMediaServiceTest {

    private final RoomMediaRepository repository = mock(RoomMediaRepository.class);
    private final RoomMediaStorage storage = mock(RoomMediaStorage.class);
    private final RoomMediaProperties properties = new RoomMediaProperties();
    private final RoomMediaService service = new RoomMediaService(repository, storage, properties);
    private final AuthenticatedUser admin = new AuthenticatedUser(1, "admin", "Admin", "ADMIN");

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void acceptsSeveralImagesAndVideosForOneRoom() throws Exception {
        when(repository.roomExists(7)).thenReturn(true);
        when(storage.store(eq(7L), anyString(), anyString(), any(UUID.class), any()))
                .thenAnswer(invocation -> {
                    UUID fileId = invocation.getArgument(3);
                    return new StoredFile(fileId + ".media", "/media-files/rooms/7/" + fileId, Path.of("media"));
                });
        when(repository.insert(eq(7L), anyString(), anyString(), any(StoredFile.class),
                anyString(), anyLong(), eq(1L)))
                .thenAnswer(invocation -> new RoomMediaResponse(
                        1, 7, invocation.getArgument(1), invocation.getArgument(2), "stored.media",
                        "/media-files/rooms/7/stored.media", invocation.getArgument(4), invocation.getArgument(5),
                        0, false, null, Instant.EPOCH, 1L
                ));
        TransactionSynchronizationManager.initSynchronization();

        List<RoomMediaResponse> uploaded = service.upload(7, List.of(
                file("one.jpg", "image/jpeg", new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff}),
                file("two.png", "image/png", new byte[]{
                        (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
                }),
                file("three.webp", "image/webp", asciiBytes("RIFF0000WEBP")),
                file("four.mp4", "video/mp4", asciiBytes("0000ftypisom")),
                file("five.webm", "video/webm", new byte[]{0x1a, 0x45, (byte) 0xdf, (byte) 0xa3})
        ), admin);

        assertEquals(5, uploaded.size());
        assertEquals(3, uploaded.stream().filter(item -> item.mediaType().equals("IMAGE")).count());
        assertEquals(2, uploaded.stream().filter(item -> item.mediaType().equals("VIDEO")).count());
        verify(storage, org.mockito.Mockito.times(5)).store(eq(7L), anyString(), anyString(), any(UUID.class), any());
    }

    @Test
    void rejectsSpoofedMimeTypeBeforeWritingFile() throws Exception {
        when(repository.roomExists(7)).thenReturn(true);
        TransactionSynchronizationManager.initSynchronization();

        assertThrows(AuthException.class, () -> service.upload(7, List.of(
                file("malware.jpg", "image/jpeg", asciiBytes("not an image"))
        ), admin));

        verify(storage, never()).store(anyLong(), anyString(), anyString(), any(UUID.class), any());
    }

    @Test
    void tenantCannotReadRoomWithoutPublicAvailabilityOrBooking() {
        when(repository.roomExists(7)).thenReturn(true);
        when(repository.isRoomPubliclyAvailable(7)).thenReturn(false);
        when(repository.tenantCanViewRoom(7, 42)).thenReturn(false);

        assertThrows(AuthException.class, () -> service.list(
                7, new AuthenticatedUser(42, "tenant", "Tenant", "NGUOI_THUE")));
    }

    private MockMultipartFile file(String name, String contentType, byte[] bytes) {
        return new MockMultipartFile("files", name, contentType, bytes);
    }

    private byte[] asciiBytes(String value) {
        return value.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }
}
