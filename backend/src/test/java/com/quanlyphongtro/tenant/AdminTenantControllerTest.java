package com.quanlyphongtro.tenant;

import com.quanlyphongtro.auth.AuthService;
import com.quanlyphongtro.config.SecurityConfig;
import com.quanlyphongtro.tenant.AdminTenantModels.PageResponse;
import com.quanlyphongtro.tenant.AdminTenantModels.TenantSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminTenantController.class)
@Import(SecurityConfig.class)
class AdminTenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminTenantService tenantService;

    @MockBean
    private AuthService authService;

    @Test
    void tenantListRequiresAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/nguoi-thue"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "NGUOI_THUE")
    void tenantCannotListOtherTenantProfiles() throws Exception {
        mockMvc.perform(get("/api/v1/admin/nguoi-thue"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanSearchTenantSummariesWithoutCitizenIds() throws Exception {
        when(tenantService.list(0, 20, "Nguyen")).thenReturn(new PageResponse<>(
                List.of(new TenantSummary(7, 12, "tenant7", "Nguyễn An", "an@example.test",
                        "0900000000", "HOAT_DONG", LocalDate.of(1995, 4, 12), "Hà Nội", "THUONG", null)),
                0, 20, 1, 1
        ));

        mockMvc.perform(get("/api/v1/admin/nguoi-thue").param("search", "Nguyen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("tenant7"))
                .andExpect(jsonPath("$.content[0].fullName").value("Nguyễn An"))
                .andExpect(jsonPath("$.content[0].citizenId").doesNotExist())
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[0].planCode").value("THUONG"))
                .andExpect(jsonPath("$.content[0].planExpiresOn").value(nullValue()))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(tenantService).list(0, 20, "Nguyen");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsInvalidPaginationAndOversizedSearch() throws Exception {
        mockMvc.perform(get("/api/v1/admin/nguoi-thue").param("page", "-1"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/admin/nguoi-thue").param("size", "101"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/admin/nguoi-thue").param("search", "x".repeat(151)))
                .andExpect(status().isBadRequest());
    }
}
