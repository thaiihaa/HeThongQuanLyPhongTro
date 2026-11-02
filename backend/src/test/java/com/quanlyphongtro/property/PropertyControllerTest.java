package com.quanlyphongtro.property;

import com.quanlyphongtro.config.SecurityConfig;
import com.quanlyphongtro.auth.AuthService;
import com.quanlyphongtro.property.PropertyModels.KhuTroResponse;
import com.quanlyphongtro.property.PropertyModels.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PropertyController.class)
@Import(SecurityConfig.class)
class PropertyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PropertyService propertyService;

    @MockBean
    private AuthService authService;

    @Test
    void propertyApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/khu-tro"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "NGUOI_THUE")
    void tenantCannotAccessPropertyManagementApi() throws Exception {
        mockMvc.perform(get("/api/v1/admin/khu-tro"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListProperties() throws Exception {
        when(propertyService.listKhuTro(0, 20, null)).thenReturn(new PageResponse<>(
                List.of(new KhuTroResponse(4L, "KHU-01", "Khu A", "Đường số 1", null, "HOAT_DONG", Instant.EPOCH, Instant.EPOCH)),
                0, 20, 1, 1
        ));

        mockMvc.perform(get("/api/v1/admin/khu-tro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tenKhuTro").value("Khu A"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsInvalidRoomStatusFilter() throws Exception {
        mockMvc.perform(get("/api/v1/admin/phong-tro").param("status", "UNSUPPORTED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
