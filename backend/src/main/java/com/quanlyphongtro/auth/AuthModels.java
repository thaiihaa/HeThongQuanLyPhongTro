package com.quanlyphongtro.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;

public final class AuthModels {

    private AuthModels() {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 100) String username,
            @Email @Size(max = 254) String email,
            @Pattern(regexp = "^$|^0[0-9]{9,10}$", message = "Số điện thoại chưa đúng định dạng.") @Size(max = 20) String phone,
            @NotBlank @Size(min = 12, max = 72) String password,
            @NotBlank @Size(min = 12, max = 72) String confirmPassword,
            @NotBlank @Size(max = 150) String fullName,
            @Pattern(regexp = "^$|^[0-9]{9,12}$", message = "Số CCCD chỉ gồm 9-12 chữ số.") @Size(max = 20) String citizenId,
            LocalDate birthDate,
            @Size(max = 500) String address
    ) {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 254) String login,
            @NotBlank @Size(max = 72) String password
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank @Size(max = 72) String currentPassword,
            @NotBlank @Size(min = 12, max = 72) String newPassword,
            @NotBlank @Size(min = 12, max = 72) String confirmPassword
    ) {
    }

    public record UserResponse(long id, String username, String fullName, String role) {
    }

    public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, UserResponse user) {
    }

    public record AuthenticatedUser(long id, String username, String fullName, String role) {
    }

    public record ApiMessage(String code, String message) {
    }
}
