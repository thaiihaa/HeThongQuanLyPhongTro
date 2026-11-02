package com.quanlyphongtro.booking;

import com.quanlyphongtro.auth.AuthModels.AuthenticatedUser;
import com.quanlyphongtro.booking.BookingModels.AdminBookingResponse;
import com.quanlyphongtro.booking.BookingModels.BookingCreatedResponse;
import com.quanlyphongtro.booking.BookingModels.BookingRequest;
import com.quanlyphongtro.booking.BookingModels.RoomResponse;
import com.quanlyphongtro.booking.BookingModels.StatusRequest;
import com.quanlyphongtro.booking.BookingModels.TenantBookingResponse;
import com.quanlyphongtro.property.PropertyModels.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Validated
@RestController
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/api/v1/rooms")
    public PageResponse<RoomResponse> listAvailableRooms(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal minPrice,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal maxPrice,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal minArea,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal maxArea
    ) {
        return bookingService.listAvailableRooms(page, size, search, minPrice, maxPrice, minArea, maxArea);
    }

    @PostMapping("/api/v1/tenant/dat-phong")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingCreatedResponse createBooking(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody BookingRequest request
    ) {
        return bookingService.createBooking(user, request);
    }

    @GetMapping("/api/v1/tenant/dat-phong")
    public PageResponse<TenantBookingResponse> listTenantBookings(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return bookingService.listTenantBookings(user, page, size);
    }

    @GetMapping("/api/v1/admin/dat-phong")
    public PageResponse<AdminBookingResponse> listAdminBookings(
            @RequestParam(required = false)
            @Pattern(regexp = "CHO_XAC_NHAN|DA_XAC_NHAN|DA_HUY|HOAN_TAT") String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return bookingService.listAdminBookings(status, page, size);
    }

    @PutMapping("/api/v1/admin/dat-phong/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateStatus(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable @Min(1) long id,
            @Valid @RequestBody StatusRequest request
    ) {
        bookingService.updateStatus(user, id, request);
    }
}
