package com.quanlyphongtro.tenant;

import com.quanlyphongtro.auth.AuthModels.AuthenticatedUser;
import com.quanlyphongtro.tenant.TenantRoomModels.TenantRoomResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenant/my-room")
public class TenantRoomController {

    private final TenantRoomService service;

    public TenantRoomController(TenantRoomService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<TenantRoomResponse> getMyRoom(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(service.findMyRoom(user.id()));
    }
}
