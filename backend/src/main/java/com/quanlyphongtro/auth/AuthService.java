package com.quanlyphongtro.auth;

import com.quanlyphongtro.auth.AuthModels.AuthResponse;
import com.quanlyphongtro.auth.AuthModels.AuthenticatedUser;
import com.quanlyphongtro.auth.AuthModels.ChangePasswordRequest;
import com.quanlyphongtro.auth.AuthModels.LoginRequest;
import com.quanlyphongtro.auth.AuthModels.RegisterRequest;
import com.quanlyphongtro.auth.AuthModels.UserResponse;
import com.quanlyphongtro.auth.AuthRepository.AccountRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class AuthService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final long expirationSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            AuthRepository authRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.session.expiration-seconds:43200}") long expirationSeconds
    ) {
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("Session expiration must be positive.");
        }
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
        this.expirationSeconds = expirationSeconds;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH", "Mật khẩu xác nhận không khớp.");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "PASSWORD_TOO_LONG", "Mật khẩu vượt quá độ dài được hỗ trợ.");
        }

        long tenantRoleId = authRepository.findRoleId("NGUOI_THUE")
                .orElseThrow(() -> new IllegalStateException("Required NGUOI_THUE role is missing."));
        String username = request.username().trim();
        String email = normalizeOptional(request.email());
        String phone = normalizeOptional(request.phone());
        String passwordHash = passwordEncoder.encode(request.password());
        long userId = authRepository.insertUser(
                tenantRoleId,
                username,
                email,
                phone,
                passwordHash,
                request.fullName().trim()
        );
        authRepository.insertTenantProfile(
                userId,
                normalizeOptional(request.citizenId()),
                request.birthDate(),
                normalizeOptional(request.address())
        );
        return createSession(userId, username, request.fullName().trim(), "NGUOI_THUE");
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String login = request.login().trim();
        Optional<AccountRecord> account = authRepository.findAccountByLogin(login);
        if (account.isEmpty()
                || !"HOAT_DONG".equals(account.get().status())
                || !passwordEncoder.matches(request.password(), account.get().passwordHash())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Tên đăng nhập hoặc mật khẩu không đúng.");
        }

        AccountRecord authenticated = account.get();
        return createSession(authenticated.id(), authenticated.username(), authenticated.fullName(), authenticated.role());
    }

    @Transactional(readOnly = true)
    public Optional<AuthenticatedUser> authenticate(String token) {
        if (!StringUtils.hasText(token) || token.length() > 128) {
            return Optional.empty();
        }
        return authRepository.findActiveUserByTokenHash(hashToken(token));
    }

    @Transactional
    public void logout(String token) {
        if (StringUtils.hasText(token) && token.length() <= 128) {
            authRepository.revokeSession(hashToken(token));
        }
    }

    @Transactional
    public void changePassword(AuthenticatedUser user, ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH", "Mật khẩu xác nhận không khớp.");
        }
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "PASSWORD_TOO_LONG", "Mật khẩu vượt quá độ dài được hỗ trợ.");
        }

        String currentHash = authRepository.findPasswordHash(user.id())
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Không tìm thấy tài khoản."));
        if (!passwordEncoder.matches(request.currentPassword(), currentHash)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "CURRENT_PASSWORD_INVALID", "Mật khẩu hiện tại không đúng.");
        }
        if (passwordEncoder.matches(request.newPassword(), currentHash)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "PASSWORD_UNCHANGED", "Mật khẩu mới phải khác mật khẩu hiện tại.");
        }
        if (authRepository.updatePasswordHash(
                user.id(), currentHash, passwordEncoder.encode(request.newPassword())) != 1) {
            throw new AuthException(HttpStatus.CONFLICT, "PASSWORD_UPDATE_CONFLICT", "Không thể cập nhật mật khẩu. Vui lòng thử lại.");
        }
        authRepository.revokeAllActiveSessions(user.id());
    }

    private AuthResponse createSession(long userId, String username, String fullName, String role) {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        Instant expiresAt = Instant.now().plusSeconds(expirationSeconds);
        authRepository.insertSession(userId, hashToken(token), expiresAt);
        return new AuthResponse(token, "Bearer", expiresAt, new UserResponse(userId, username, fullName, role));
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }
}
