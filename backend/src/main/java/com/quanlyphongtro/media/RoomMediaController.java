package com.quanlyphongtro.media;

import com.quanlyphongtro.auth.AuthModels.AuthenticatedUser;
import com.quanlyphongtro.media.RoomMediaModels.DescriptionRequest;
import com.quanlyphongtro.media.RoomMediaModels.OrderRequest;
import com.quanlyphongtro.media.RoomMediaModels.RoomMediaResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Validated
@RestController
public class RoomMediaController {

    private final RoomMediaService service;

    public RoomMediaController(RoomMediaService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/rooms/{roomId}/media")
    public List<RoomMediaResponse> listPublicRoomMedia(
            @PathVariable @Min(1) long roomId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return service.list(roomId, user);
    }

    @GetMapping("/api/v1/admin/phong-tro/{roomId}/media")
    public List<RoomMediaResponse> listAdminRoomMedia(
            @PathVariable @Min(1) long roomId,
            @AuthenticationPrincipal AuthenticatedUser admin
    ) {
        return service.list(roomId, admin);
    }

    @PostMapping("/api/v1/admin/phong-tro/{roomId}/media")
    @ResponseStatus(HttpStatus.CREATED)
    public List<RoomMediaResponse> upload(
            @PathVariable @Min(1) long roomId,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal AuthenticatedUser admin
    ) {
        return service.upload(roomId, files, admin);
    }

    @DeleteMapping("/api/v1/admin/phong-tro/{roomId}/media/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable @Min(1) long roomId,
            @PathVariable @Min(1) long mediaId,
            @AuthenticationPrincipal AuthenticatedUser admin
    ) {
        service.delete(roomId, mediaId, admin);
    }

    @PutMapping("/api/v1/admin/phong-tro/{roomId}/media/{mediaId}/primary")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPrimary(
            @PathVariable @Min(1) long roomId,
            @PathVariable @Min(1) long mediaId,
            @AuthenticationPrincipal AuthenticatedUser admin
    ) {
        service.setPrimary(roomId, mediaId, admin);
    }

    @PutMapping("/api/v1/admin/phong-tro/{roomId}/media/{mediaId}/order")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateOrder(
            @PathVariable @Min(1) long roomId,
            @PathVariable @Min(1) long mediaId,
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin
    ) {
        service.updateOrder(roomId, mediaId, request, admin);
    }

    @PutMapping("/api/v1/admin/phong-tro/{roomId}/media/{mediaId}/description")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateDescription(
            @PathVariable @Min(1) long roomId,
            @PathVariable @Min(1) long mediaId,
            @Valid @RequestBody DescriptionRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin
    ) {
        service.updateDescription(roomId, mediaId, request, admin);
    }
}
