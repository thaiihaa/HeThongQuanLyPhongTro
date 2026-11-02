package com.quanlyphongtro.auth;

import com.quanlyphongtro.auth.AuthModels.AuthResponse;
import com.quanlyphongtro.auth.AuthModels.AuthenticatedUser;
import com.quanlyphongtro.auth.AuthModels.ChangePasswordRequest;
import com.quanlyphongtro.auth.AuthModels.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private AuthRepository authRepository;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authRepository = mock(AuthRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authService = new AuthService(authRepository, passwordEncoder, 3600);
    }

    @Test
    void registerAlwaysCreatesTenantAndStoresOnlyHashedSessionToken() {
        RegisterRequest request = new RegisterRequest(
                " tenant01 ",
                "tenant@example.test",
                "0900000000",
                "a-long-password-123",
                "a-long-password-123",
                "Nguyen Van A",
                null,
                LocalDate.of(1998, 1, 2),
                "Dia chi"
        );
        when(authRepository.findRoleId("NGUOI_THUE")).thenReturn(Optional.of(4L));
        when(passwordEncoder.encode(request.password())).thenReturn("bcrypt-hash");
        when(authRepository.insertUser(4L, "tenant01", "tenant@example.test", "0900000000", "bcrypt-hash", "Nguyen Van A"))
                .thenReturn(42L);

        AuthResponse response = authService.register(request);

        assertEquals("NGUOI_THUE", response.user().role());
        assertEquals("tenant01", response.user().username());
        assertEquals("Bearer", response.tokenType());
        assertNotEquals(response.accessToken(), "bcrypt-hash");
        verify(authRepository).insertTenantProfile(42L, null, LocalDate.of(1998, 1, 2), "Dia chi");
        org.mockito.ArgumentCaptor<String> storedTokenHash = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(authRepository).insertSession(eq(42L), storedTokenHash.capture(), eq(response.expiresAt()));
        assertEquals(64, storedTokenHash.getValue().length());
        assertNotEquals(response.accessToken(), storedTokenHash.getValue());
        verify(passwordEncoder).encode(request.password());
    }

    @Test
    void registerRejectsMismatchedPasswordsBeforeWriting() {
        RegisterRequest request = new RegisterRequest(
                "tenant01", null, null, "a-long-password-123", "a-different-password",
                "Nguyen Van A", null, null, null
        );

        AuthException exception = assertThrows(AuthException.class, () -> authService.register(request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        assertEquals("PASSWORD_MISMATCH", exception.code());
        verify(authRepository, never()).insertUser(
                eq(4L), anyString(), anyString(), anyString(), anyString(), anyString()
        );
    }

    @Test
    void loginUsesGenericErrorForWrongPassword() {
        when(authRepository.findAccountByLogin("tenant01")).thenReturn(Optional.of(
                new AuthRepository.AccountRecord(42L, "tenant01", "bcrypt-hash", "Nguyen Van A", "HOAT_DONG", "NGUOI_THUE")
        ));
        when(passwordEncoder.matches("wrong-password", "bcrypt-hash")).thenReturn(false);

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.login(new AuthModels.LoginRequest("tenant01", "wrong-password")));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.status());
        assertEquals("INVALID_CREDENTIALS", exception.code());
        assertEquals("Tên đăng nhập hoặc mật khẩu không đúng.", exception.getMessage());
        verify(authRepository, never()).insertSession(any(Long.class), anyString(), any());
    }

    @Test
    void changePasswordHashesNewPasswordAndRevokesExistingSessions() {
        AuthenticatedUser user = new AuthenticatedUser(42L, "user", "Người thuê demo", "NGUOI_THUE");
        ChangePasswordRequest request = new ChangePasswordRequest("123456", "a-strong-new-password", "a-strong-new-password");
        when(authRepository.findPasswordHash(42L)).thenReturn(Optional.of("old-hash"));
        when(passwordEncoder.matches("123456", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("a-strong-new-password", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("a-strong-new-password")).thenReturn("new-hash");
        when(authRepository.updatePasswordHash(42L, "old-hash", "new-hash")).thenReturn(1);

        authService.changePassword(user, request);

        verify(authRepository).updatePasswordHash(42L, "old-hash", "new-hash");
        verify(authRepository).revokeAllActiveSessions(42L);
    }

    @Test
    void changePasswordRejectsInvalidCurrentPasswordWithoutUpdating() {
        AuthenticatedUser user = new AuthenticatedUser(42L, "user", "Người thuê demo", "NGUOI_THUE");
        when(authRepository.findPasswordHash(42L)).thenReturn(Optional.of("old-hash"));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        AuthException exception = assertThrows(AuthException.class, () -> authService.changePassword(user,
                new ChangePasswordRequest("wrong", "a-strong-new-password", "a-strong-new-password")));

        assertEquals("CURRENT_PASSWORD_INVALID", exception.code());
        verify(authRepository, never()).updatePasswordHash(org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString());
    }

    @Test
    void changePasswordRejectsMismatchedConfirmation() {
        AuthenticatedUser user = new AuthenticatedUser(42L, "user", "Người thuê demo", "NGUOI_THUE");

        AuthException exception = assertThrows(AuthException.class, () -> authService.changePassword(user,
                new ChangePasswordRequest("123456", "a-strong-new-password", "a-different-new-password")));

        assertEquals("PASSWORD_MISMATCH", exception.code());
        verifyNoInteractions(authRepository);
    }
}
