package com.quanlyphongtro.booking;

import com.quanlyphongtro.auth.AuthException;
import com.quanlyphongtro.auth.AuthModels.AuthenticatedUser;
import com.quanlyphongtro.booking.BookingModels.AdminBookingResponse;
import com.quanlyphongtro.booking.BookingModels.BookingRequest;
import com.quanlyphongtro.booking.BookingModels.BookingCreatedResponse;
import com.quanlyphongtro.booking.BookingModels.RoomResponse;
import com.quanlyphongtro.booking.BookingModels.StatusRequest;
import com.quanlyphongtro.booking.BookingModels.TenantBookingResponse;
import com.quanlyphongtro.property.PropertyModels.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

@Service
public class BookingService {

    private final BookingRepository repository;

    public BookingService(BookingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResponse<RoomResponse> listAvailableRooms(
            int page, int size, String search, BigDecimal minPrice, BigDecimal maxPrice,
            BigDecimal minArea, BigDecimal maxArea
    ) {
        validateRange(minPrice, maxPrice, "Giá thuê tối thiểu không được lớn hơn giá tối đa.");
        validateRange(minArea, maxArea, "Diện tích tối thiểu không được lớn hơn diện tích tối đa.");
        PageRequest request = page(page, size);
        return repository.listAvailableRooms(
                request.page(), request.size(), normalizeSearch(search), minPrice, maxPrice, minArea, maxArea
        );
    }

    @Transactional
    public BookingCreatedResponse createBooking(AuthenticatedUser user, BookingRequest request) {
        validateDates(request.expectedCheckIn(), request.expectedCheckOut());
        if (!repository.lockAvailableRoom(request.roomId())) {
            throw conflict("Phòng không còn trống hoặc không tồn tại.");
        }
        if (repository.hasActiveLeaseConflict(request.roomId(), request.expectedCheckIn(), request.expectedCheckOut())
                || repository.hasBookingConflict(
                        request.roomId(), request.expectedCheckIn(), request.expectedCheckOut(), null
                )) {
            throw conflict("Phòng đã có hợp đồng hoặc yêu cầu đặt trùng thời gian.");
        }

        long bookingId = repository.insertBooking(user.id(), new BookingRepository.BookingRequestValue(
                request.roomId(), request.phone().trim(), normalizeOptional(request.email()),
                request.expectedCheckIn(), request.expectedCheckOut(), normalizeOptional(request.note())
        ));
        repository.insertAudit(user.id(), "TAO_YEU_CAU_DAT_PHONG", bookingId, null, "CHO_XAC_NHAN");
        return new BookingCreatedResponse(bookingId, "CHO_XAC_NHAN");
    }

    @Transactional(readOnly = true)
    public PageResponse<TenantBookingResponse> listTenantBookings(AuthenticatedUser user, int page, int size) {
        PageRequest request = page(page, size);
        return repository.listTenantBookings(user.id(), request.page(), request.size());
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminBookingResponse> listAdminBookings(String status, int page, int size) {
        PageRequest request = page(page, size);
        return repository.listAdminBookings(normalizeStatus(status),
                request.page(), request.size());
    }

    @Transactional
    public void updateStatus(AuthenticatedUser admin, long bookingId, StatusRequest request) {
        String targetStatus = normalizeStatus(request.status());
        BookingRepository.BookingRecord snapshot = repository.findBooking(bookingId)
                .orElseThrow(() -> notFound("Không tìm thấy yêu cầu đặt phòng."));
        if (!repository.lockRoom(snapshot.roomId())) {
            throw notFound("Không tìm thấy phòng của yêu cầu đặt.");
        }
        BookingRepository.BookingRecord booking = repository.lockBooking(bookingId)
                .orElseThrow(() -> notFound("Không tìm thấy yêu cầu đặt phòng."));
        validateTransition(booking.status(), targetStatus);

        if ("DA_XAC_NHAN".equals(targetStatus)) {
            if (repository.hasActiveLeaseConflict(booking.roomId(), booking.checkIn(), booking.checkOut())
                    || repository.hasBookingConflict(
                    booking.roomId(), booking.checkIn(), booking.checkOut(), booking.id()
            )) {
                throw conflict("Không thể xác nhận vì phòng đã có hợp đồng hoặc yêu cầu trùng thời gian.");
            }
            if (!repository.lockAvailableRoom(booking.roomId())) {
                throw conflict("Không thể xác nhận vì phòng không còn trạng thái trống.");
            }
        }

        if (repository.updateBookingStatus(bookingId, booking.status(), targetStatus) != 1) {
            throw conflict("Yêu cầu đã được cập nhật bởi thao tác khác. Vui lòng tải lại danh sách.");
        }
        repository.insertAudit(admin.id(), "CAP_NHAT_DAT_PHONG", bookingId, booking.status(), targetStatus);
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn.isBefore(LocalDate.now())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE",
                    "Ngày nhận phòng dự kiến không được ở trong quá khứ.");
        }
        if (checkOut != null && !checkOut.isAfter(checkIn)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE",
                    "Ngày trả phòng dự kiến phải sau ngày nhận phòng.");
        }
    }

    private void validateTransition(String current, String target) {
        boolean allowed = switch (current) {
            case "CHO_XAC_NHAN" -> "DA_XAC_NHAN".equals(target) || "DA_HUY".equals(target);
            case "DA_XAC_NHAN" -> "DA_HUY".equals(target) || "HOAN_TAT".equals(target);
            default -> false;
        };
        if (!allowed) {
            throw conflict("Không thể chuyển yêu cầu từ trạng thái " + current + " sang " + target + ".");
        }
    }

    private void validateRange(BigDecimal min, BigDecimal max, String message) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "INVALID_FILTER_RANGE", message);
        }
    }

    private PageRequest page(int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.min(Math.max(page, 0), Integer.MAX_VALUE / safeSize);
        return new PageRequest(safePage, safeSize);
    }

    private String normalizeSearch(String search) {
        return StringUtils.hasText(search) ? search.trim() : null;
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeStatus(String status) {
        return StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : null;
    }

    private AuthException notFound(String message) {
        return new AuthException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    private AuthException conflict(String message) {
        return new AuthException(HttpStatus.CONFLICT, "BOOKING_CONFLICT", message);
    }

    private record PageRequest(int page, int size) {
    }
}
