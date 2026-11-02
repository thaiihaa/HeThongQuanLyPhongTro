package com.quanlyphongtro.booking;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class BookingModels {

    private BookingModels() {
    }

    public record RoomResponse(
            long id,
            String roomCode,
            String siteName,
            String address,
            String roomType,
            BigDecimal areaSquareMeters,
            BigDecimal monthlyRent,
            String floor,
            String description,
            String primaryImageUrl
    ) {
    }

    public record BookingRequest(
            @Positive long roomId,
            @NotBlank @Pattern(regexp = "^0[0-9]{9,10}$", message = "Số điện thoại chưa đúng định dạng.") String phone,
            @Email @Size(max = 254) String email,
            @NotNull LocalDate expectedCheckIn,
            LocalDate expectedCheckOut,
            @Size(max = 1000) String note
    ) {
    }

    public record StatusRequest(
            @NotBlank @Pattern(regexp = "DA_XAC_NHAN|DA_HUY|HOAN_TAT") String status
    ) {
    }

    public record BookingCreatedResponse(long id, String status) {
    }

    public record TenantBookingResponse(
            long id,
            long roomId,
            String roomCode,
            String siteName,
            LocalDate expectedCheckIn,
            LocalDate expectedCheckOut,
            String status,
            String note,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record AdminBookingResponse(
            long id,
            long roomId,
            String roomCode,
            String siteName,
            String tenantName,
            String phone,
            String email,
            String planCode,
            LocalDate expectedCheckIn,
            LocalDate expectedCheckOut,
            String status,
            String note,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
