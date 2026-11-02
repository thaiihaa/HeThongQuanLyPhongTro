package com.quanlyphongtro.booking;

import com.quanlyphongtro.auth.AuthService;
import com.quanlyphongtro.config.SecurityConfig;
import com.quanlyphongtro.property.PropertyModels.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BookingController.class)
@Import(SecurityConfig.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private AuthService authService;

    @Test
    void availableRoomsCanBeBrowsedWithoutAuthentication() throws Exception {
        when(bookingService.listAvailableRooms(0, 20, null, null, null, null, null))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void tenantBookingApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/tenant/dat-phong"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "NGUOI_THUE")
    void tenantCannotReadAdminBookings() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dat-phong"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotAccessTenantBookingArea() throws Exception {
        mockMvc.perform(get("/api/v1/tenant/dat-phong"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsInvalidAdminBookingStatusFilter() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dat-phong").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
