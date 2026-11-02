package com.quanlyphongtro.tenant;

import com.quanlyphongtro.tenant.TenantRoomModels.TenantRoomResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantRoomService {

    private final TenantRoomRepository repository;

    public TenantRoomService(TenantRoomRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public TenantRoomResponse findMyRoom(long userId) {
        return repository.findMyRoom(userId).orElse(null);
    }
}
