package com.quanlyphongtro.tenant;

import com.quanlyphongtro.media.RoomMediaModels.RoomMediaResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class TenantRoomModels {

    private TenantRoomModels() {
    }

    public record TenantRoomResponse(
            long roomId,
            String roomCode,
            String siteName,
            String address,
            BigDecimal areaSquareMeters,
            BigDecimal monthlyRent,
            String status,
            String description,
            LocalDate leaseStartsOn,
            LocalDate leaseEndsOn,
            List<RoomAmenityResponse> amenities,
            List<RoomAssetResponse> assets,
            List<RoomServiceResponse> services,
            List<RoomMediaResponse> media
    ) {
    }

    public record RoomAmenityResponse(String name, int quantity, String condition) {
    }

    public record RoomAssetResponse(String name, int quantity, String condition) {
    }

    public record RoomServiceResponse(
            String name,
            String unit,
            BigDecimal appliedPrice,
            String calculationMethod
    ) {
    }
}
