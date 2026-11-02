package com.quanlyphongtro.plan;

import com.quanlyphongtro.auth.AuthModels.AuthenticatedUser;
import com.quanlyphongtro.auth.AuthService;
import com.quanlyphongtro.config.SecurityConfig;
import com.quanlyphongtro.plan.PlanModels.AdminPlanResponse;
import com.quanlyphongtro.plan.PlanModels.TenantPlanResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlanController.class)
@Import(SecurityConfig.class)
class PlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlanService planService;

    @MockBean
    private AuthService authService;

    @Test
    void tenantPlanRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/tenant/plan"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCannotReadTenantPlanEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/tenant/plan").with(authenticatedUser("ADMIN", 1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void tenantReadsOwnPlanUsingAuthenticatedPrincipal() throws Exception {
        when(planService.getTenantPlan(12L)).thenReturn(new TenantPlanResponse(
                "PREMIUM", "Premium", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                true, "Standard benefits plus priority booking queue"
        ));

        mockMvc.perform(get("/api/v1/tenant/plan").with(authenticatedUser("NGUOI_THUE", 12L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PREMIUM"))
                .andExpect(jsonPath("$.premium").value(true))
                .andExpect(jsonPath("$.role").doesNotExist());

        verify(planService).getTenantPlan(12L);
    }

    @Test
    void premiumEntitlementDoesNotGiveTheTenantAdminAuthority() throws Exception {
        when(planService.getTenantPlan(12L)).thenReturn(new TenantPlanResponse(
                "PREMIUM", "Premium", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                true, "Priority booking queue"
        ));

        mockMvc.perform(get("/api/v1/tenant/plan").with(authenticatedUser("NGUOI_THUE", 12L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PREMIUM"));

        mockMvc.perform(get("/api/v1/admin/nguoi-thue/12/plan")
                        .with(authenticatedUser("NGUOI_THUE", 12L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void tenantCannotGrantOrRevokePremium() throws Exception {
        mockMvc.perform(post("/api/v1/admin/nguoi-thue/12/plan")
                        .with(authenticatedUser("NGUOI_THUE", 12L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expiresOn\":\"2026-12-01\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/admin/nguoi-thue/12/plan")
                        .with(authenticatedUser("NGUOI_THUE", 12L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminGrantsPremiumAndRemainsDistinctFromTenantTier() throws Exception {
        LocalDate expiresOn = LocalDate.now().plusDays(30);
        when(planService.grantPremium(12L, 1L, expiresOn)).thenReturn(
                new AdminPlanResponse(12L, "PREMIUM", LocalDate.now(), expiresOn)
        );

        mockMvc.perform(post("/api/v1/admin/nguoi-thue/12/plan")
                        .with(authenticatedUser("ADMIN", 1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expiresOn\":\"" + expiresOn + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PREMIUM"))
                .andExpect(jsonPath("$.tenantUserId").value(12));

        verify(planService).grantPremium(12L, 1L, expiresOn);
    }

    @Test
    void standardTenantCannotAccessAdminPlanManagement() throws Exception {
        mockMvc.perform(post("/api/v1/admin/nguoi-thue/12/plan"))
                .andExpect(status().isUnauthorized());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor authenticatedUser(
            String role, long userId
    ) {
        AuthenticatedUser user = new AuthenticatedUser(userId, "test-user", "Test User", role);
        UsernamePasswordAuthenticationToken authToken = UsernamePasswordAuthenticationToken.authenticated(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        return authentication(authToken);
    }
}
