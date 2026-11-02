package com.quanlyphongtro.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
public class BootstrapService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;

    public BootstrapService(AuthRepository authRepository, PasswordEncoder passwordEncoder) {
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public boolean createAdminIfAbsent(String username, String email, String phone, String password, String fullName) {
        return createIfAbsent("ADMIN", username, email, phone, password, fullName, false, null, null, null, 12);
    }

    @Transactional
    public boolean createTenantIfAbsent(
            String username,
            String email,
            String phone,
            String password,
            String fullName,
            String citizenId,
            LocalDate birthDate,
            String address
    ) {
        return createIfAbsent("NGUOI_THUE", username, email, phone, password, fullName, true,
                citizenId, birthDate, address, 12);
    }

    @Transactional
    public DemoBootstrapResult createDemoUsersIfAbsent(String adminPassword, String tenantPassword) {
        validatePassword(adminPassword, 6);
        validatePassword(tenantPassword, 6);
        boolean adminCreated = createIfAbsent("ADMIN", "admin", null, null, adminPassword,
                "Quản trị viên demo", false, null, null, null, 6);
        boolean tenantCreated = createIfAbsent("NGUOI_THUE", "user", null, null, tenantPassword,
                "Người thuê demo", true, null, null, null, 6);
        return new DemoBootstrapResult(adminCreated, tenantCreated);
    }

    private boolean createIfAbsent(
            String roleCode,
            String username,
            String email,
            String phone,
            String password,
            String fullName,
            boolean tenant,
            String citizenId,
            LocalDate birthDate,
            String address,
            int minimumPasswordLength
    ) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password) || !StringUtils.hasText(fullName)) {
            throw new IllegalArgumentException("Bootstrap username, password and full name are required.");
        }
        validatePassword(password, minimumPasswordLength);

        String normalizedUsername = username.trim();
        if (authRepository.findUserIdByUsername(normalizedUsername).isPresent()) {
            return false;
        }
        long roleId = authRepository.findRoleId(roleCode)
                .orElseThrow(() -> new IllegalStateException("Required role is missing: " + roleCode));
        long userId = authRepository.insertUser(
                roleId,
                normalizedUsername,
                normalizeOptional(email),
                normalizeOptional(phone),
                passwordEncoder.encode(password),
                fullName.trim()
        );
        if (tenant) {
            authRepository.insertTenantProfile(userId, normalizeOptional(citizenId), birthDate, normalizeOptional(address));
        }
        return true;
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void validatePassword(String password, int minimumLength) {
        if (!StringUtils.hasText(password)
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < minimumLength
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Bootstrap password is outside the allowed UTF-8 byte length.");
        }
    }

    public record DemoBootstrapResult(boolean adminCreated, boolean tenantCreated) {
    }
}
