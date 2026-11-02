package com.quanlyphongtro.dashboard;

import com.quanlyphongtro.auth.AuthService;
import com.quanlyphongtro.config.SecurityConfig;
import com.quanlyphongtro.dashboard.DashboardModels.AdminDashboardResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminDashboardController.class)
@Import(SecurityConfig.class)
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardRepository dashboardRepository;

    @MockBean
    private AuthService authService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void dashboardReturnsDatabaseBackedRoomMetrics() throws Exception {
        when(dashboardRepository.loadAdminDashboard()).thenReturn(
                new AdminDashboardResponse(12, 10, 2, 1, 8, 3, 20, 5, 11)
        );

        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSites").value(3))
                .andExpect(jsonPath("$.totalRooms").value(20))
                .andExpect(jsonPath("$.vacantRooms").value(5))
                .andExpect(jsonPath("$.occupiedRooms").value(11))
                .andExpect(jsonPath("$.totalAccounts").value(12));
    }

    @Test
    @WithMockUser(roles = "NGUOI_THUE")
    void tenantCannotReadAdminDashboard() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isForbidden());
    }
}
