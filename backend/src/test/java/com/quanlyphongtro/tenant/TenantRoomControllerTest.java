package com.quanlyphongtro.tenant;

import com.quanlyphongtro.auth.AuthModels.AuthenticatedUser;
import com.quanlyphongtro.auth.AuthService;
import com.quanlyphongtro.config.SecurityConfig;
import com.quanlyphongtro.tenant.TenantRoomModels.TenantRoomResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TenantRoomController.class)
@Import(SecurityConfig.class)
class TenantRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TenantRoomService service;

    @MockBean
    private AuthService authService;

    @Test
    void myRoomRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/tenant/my-room"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCannotUseTenantRoomEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/tenant/my-room").with(authenticatedUser("ADMIN", 1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void tenantGetsRoomBasedOnAuthenticatedUserWithoutRoomIdInput() throws Exception {
        when(service.findMyRoom(12L)).thenReturn(new TenantRoomResponse(
                101L, "P101", "Khu trọ", "Địa chỉ", new BigDecimal("18.00"),
                new BigDecimal("1800000.00"), "DANG_THUE", "Mô tả",
                LocalDate.of(2026, 10, 1), LocalDate.of(2027, 9, 30),
                List.of(), List.of(), List.of(), List.of()
        ));

        mockMvc.perform(get("/api/v1/tenant/my-room?roomId=999")
                        .with(authenticatedUser("NGUOI_THUE", 12L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomCode").value("P101"))
                .andExpect(jsonPath("$.status").value("DANG_THUE"))
                .andExpect(jsonPath("$.media").isArray());

        verify(service).findMyRoom(12L);
    }

    @Test
    void tenantWithoutAnActiveLeaseGetsAnEmptyResponse() throws Exception {
        when(service.findMyRoom(12L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/tenant/my-room").with(authenticatedUser("NGUOI_THUE", 12L)))
                .andExpect(status().isOk())
                .andExpect(content -> org.junit.jupiter.api.Assertions.assertTrue(
                        content.getResponse().getContentAsString().isEmpty()
                ));

        verify(service).findMyRoom(12L);
    }

    @Test
    void premiumTenantRoleDoesNotGrantAdminAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .with(authenticatedUser("PREMIUM", 12L)))
                .andExpect(status().isForbidden());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor authenticatedUser(
            String role, long userId
    ) {
        AuthenticatedUser user = new AuthenticatedUser(userId, "tenant", "Tenant", role);
        UsernamePasswordAuthenticationToken authToken = UsernamePasswordAuthenticationToken.authenticated(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        return authentication(authToken);
    }
}
