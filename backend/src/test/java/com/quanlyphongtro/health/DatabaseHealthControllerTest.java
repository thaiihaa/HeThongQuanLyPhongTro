package com.quanlyphongtro.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.quanlyphongtro.config.SecurityConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import com.quanlyphongtro.auth.AuthService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DatabaseHealthController.class)
@Import(SecurityConfig.class)
class DatabaseHealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DatabaseHealthService databaseHealthService;

        @MockBean
        private AuthService authService;

    @Test
    void databaseHealth_returnsUpWhenServiceHealthy() throws Exception {
        when(databaseHealthService.check())
                .thenReturn(com.quanlyphongtro.health.dto.DatabaseHealthResponse.up("QuanLyPhongTro"));

        mockMvc.perform(get("/api/v1/health/database"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("QuanLyPhongTro"));
    }

    @Test
    void databaseHealth_returnsServiceUnavailableWhenServiceDown() throws Exception {
        when(databaseHealthService.check())
                .thenReturn(com.quanlyphongtro.health.dto.DatabaseHealthResponse.down("DRIVER_OR_INTEGRATED_AUTH_ERROR"));

        mockMvc.perform(get("/api/v1/health/database"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.errorCode").value("DRIVER_OR_INTEGRATED_AUTH_ERROR"));
    }
}
