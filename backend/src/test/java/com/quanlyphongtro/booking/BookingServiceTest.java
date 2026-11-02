package com.quanlyphongtro.booking;

import com.quanlyphongtro.auth.AuthException;
import com.quanlyphongtro.auth.AuthModels.AuthenticatedUser;
import com.quanlyphongtro.booking.BookingModels.BookingRequest;
import com.quanlyphongtro.booking.BookingModels.StatusRequest;
import com.quanlyphongtro.booking.BookingModels.TenantBookingResponse;
import com.quanlyphongtro.property.PropertyModels.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingServiceTest {

    private final BookingRepository repository = mock(BookingRepository.class);
    private final BookingService service = new BookingService(repository);
    private final AuthenticatedUser tenant = new AuthenticatedUser(12, "tenant", "Người thuê", "NGUOI_THUE");

    @Test
    void createsBookingOnlyForAvailableRoomWithoutOverlappingLeaseOrBooking() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(10);
        when(repository.lockAvailableRoom(8)).thenReturn(true);
        when(repository.hasActiveLeaseConflict(8, checkIn, checkOut)).thenReturn(false);
        when(repository.hasBookingConflict(8, checkIn, checkOut, null)).thenReturn(false);
        when(repository.insertBooking(any(Long.class), any())).thenReturn(31L);

        var response = service.createBooking(tenant, new BookingRequest(
                8, "0901234567", "tenant@example.test", checkIn, checkOut, "Gọi trước khi đến"
        ));

        assertEquals(31L, response.id());
        assertEquals("CHO_XAC_NHAN", response.status());
        verify(repository).insertAudit(12, "TAO_YEU_CAU_DAT_PHONG", 31, null, "CHO_XAC_NHAN");
    }

    @Test
    void rejectsBookingWhenRoomIsNoLongerAvailable() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        when(repository.lockAvailableRoom(8)).thenReturn(false);

        AuthException exception = assertThrows(AuthException.class, () -> service.createBooking(tenant,
                new BookingRequest(8, "0901234567", null, checkIn, null, null)));

        assertEquals(HttpStatus.CONFLICT, exception.status());
        verify(repository, never()).insertBooking(any(Long.class), any());
    }

    @Test
    void rejectsCheckoutThatDoesNotFollowCheckin() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        AuthException exception = assertThrows(AuthException.class, () -> service.createBooking(tenant,
                new BookingRequest(8, "0901234567", null, checkIn, checkIn, null)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        verify(repository, never()).lockAvailableRoom(8);
    }

    @Test
    void rejectsApprovalWhenAnotherBookingOverlaps() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(10);
        when(repository.findBooking(19)).thenReturn(Optional.of(new BookingRepository.BookingRecord(
                19, 8, checkIn, checkOut, "CHO_XAC_NHAN"
        )));
        when(repository.lockRoom(8)).thenReturn(true);
        when(repository.lockBooking(19)).thenReturn(Optional.of(new BookingRepository.BookingRecord(
                19, 8, checkIn, checkOut, "CHO_XAC_NHAN"
        )));
        when(repository.hasBookingConflict(8, checkIn, checkOut, 19L)).thenReturn(true);

        AuthException exception = assertThrows(AuthException.class, () -> service.updateStatus(
                new AuthenticatedUser(1, "admin", "Admin", "ADMIN"), 19, new StatusRequest("DA_XAC_NHAN")
        ));

        assertEquals(HttpStatus.CONFLICT, exception.status());
        verify(repository, never()).updateBookingStatus(19, "CHO_XAC_NHAN", "DA_XAC_NHAN");
    }

    @Test
    void rejectsCheckinInThePast() {
        AuthException exception = assertThrows(AuthException.class, () -> service.createBooking(tenant,
                new BookingRequest(8, "0901234567", null, LocalDate.now().minusDays(1), null, null)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        verify(repository, never()).lockAvailableRoom(8);
    }

    @Test
    void tenantBookingHistoryIsAlwaysLoadedForTheAuthenticatedUserId() {
        when(repository.listTenantBookings(12L, 0, 20))
                .thenReturn(new PageResponse<TenantBookingResponse>(java.util.List.of(), 0, 20, 0, 0));

        service.listTenantBookings(tenant, 0, 20);

        verify(repository).listTenantBookings(12L, 0, 20);
    }
}
