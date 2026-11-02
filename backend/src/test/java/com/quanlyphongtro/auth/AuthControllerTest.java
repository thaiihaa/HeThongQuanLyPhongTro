package com.quanlyphongtro.auth;

import com.quanlyphongtro.auth.AuthModels.AuthResponse;
import com.quanlyphongtro.auth.AuthModels.LoginRequest;
import com.quanlyphongtro.auth.AuthModels.RegisterRequest;
import com.quanlyphongtro.auth.AuthModels.UserResponse;
import com.quanlyphongtro.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void registerIsPublicAndReturnsCreated() throws Exception {
        AuthResponse response = new AuthResponse(
                "one-time-token",
                "Bearer",
                Instant.parse("2030-01-01T00:00:00Z"),
                new UserResponse(8L, "tenant01", "Nguyen Van A", "NGUOI_THUE")
        );
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"tenant01",
                                  "email":"tenant@example.test",
                                  "phone":"0900000000",
                                  "password":"a-long-password-123",
                                  "confirmPassword":"a-long-password-123",
                                  "fullName":"Nguyen Van A",
                                  "birthDate":"1998-01-02"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("NGUOI_THUE"))
                .andExpect(jsonPath("$.accessToken").value("one-time-token"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void loginIsPublic() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(new AuthResponse(
                "one-time-token",
                "Bearer",
                Instant.parse("2030-01-01T00:00:00Z"),
                new UserResponse(8L, "tenant01", "Nguyen Van A", "NGUOI_THUE")
        ));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"tenant01","password":"a-long-password-123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void currentUserRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changingPasswordRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"123456","newPassword":"a-strong-password-123",
                                 "confirmPassword":"a-strong-password-123"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "NGUOI_THUE")
    void authenticatedUserCanChangePassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"123456","newPassword":"a-strong-password-123",
                                 "confirmPassword":"a-strong-password-123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PASSWORD_CHANGED"));

        verify(authService).changePassword(any(), any());
    }

    @Test
    @WithMockUser(roles = "NGUOI_THUE")
    void tenantCannotAccessAdminApi() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotAccessTenantOwnedApi() throws Exception {
        mockMvc.perform(get("/api/v1/tenant/dashboard"))
                .andExpect(status().isForbidden());
    }
}
