package com.quanlyphongtro.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BootstrapServiceTest {

    @Test
    void createsDemoUsersWithHashedPasswordsAndLinkedTenantProfile() {
        AuthRepository repository = mock(AuthRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(repository.findRoleId("ADMIN")).thenReturn(Optional.of(1L));
        when(repository.findRoleId("NGUOI_THUE")).thenReturn(Optional.of(4L));
        when(repository.insertUser(eq(1L), eq("admin"), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), eq("admin-hash"), eq("Quản trị viên demo"))).thenReturn(10L);
        when(repository.insertUser(eq(4L), eq("user"), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), eq("tenant-hash"), eq("Người thuê demo"))).thenReturn(11L);
        when(passwordEncoder.encode("123456")).thenReturn("admin-hash", "tenant-hash");
        BootstrapService service = new BootstrapService(repository, passwordEncoder);

        BootstrapService.DemoBootstrapResult result = service.createDemoUsersIfAbsent("123456", "123456");

        assertTrue(result.adminCreated());
        assertTrue(result.tenantCreated());
        verify(repository).insertTenantProfile(11L, null, null, null);
        verify(passwordEncoder, org.mockito.Mockito.times(2)).encode("123456");
    }

    @Test
    void demoPasswordsMustMeetMinimumLengthAndBcryptLimit() {
        AuthRepository repository = mock(AuthRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        BootstrapService service = new BootstrapService(repository, passwordEncoder);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.createDemoUsersIfAbsent("12345", "123456"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.createDemoUsersIfAbsent("123456", "x".repeat(73)));
    }

    @Test
    void existingBootstrapUsernameIsLeftUnchanged() {
        AuthRepository repository = mock(AuthRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(repository.findUserIdByUsername("admin01")).thenReturn(Optional.of(99L));
        BootstrapService service = new BootstrapService(repository, passwordEncoder);

        boolean created = service.createAdminIfAbsent(
                "admin01", "admin@example.test", null, "a-long-bootstrap-password", "Admin User"
        );

        assertFalse(created);
        verify(repository).findUserIdByUsername("admin01");
        verify(repository, never()).findRoleId("ADMIN");
        verify(repository, never()).insertUser(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
        verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.anyString());
    }
}
