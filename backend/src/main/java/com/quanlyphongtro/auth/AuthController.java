package com.quanlyphongtro.auth;

import com.quanlyphongtro.auth.AuthModels.AuthResponse;
import com.quanlyphongtro.auth.AuthModels.AuthenticatedUser;
import com.quanlyphongtro.auth.AuthModels.ChangePasswordRequest;
import com.quanlyphongtro.auth.AuthModels.ApiMessage;
import com.quanlyphongtro.auth.AuthModels.LoginRequest;
import com.quanlyphongtro.auth.AuthModels.RegisterRequest;
import com.quanlyphongtro.auth.AuthModels.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse currentUser(@AuthenticationPrincipal AuthenticatedUser user) {
        return new UserResponse(user.id(), user.username(), user.fullName(), user.role());
    }

    @PostMapping("/change-password")
    public ApiMessage changePassword(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(user, request);
        return new ApiMessage("PASSWORD_CHANGED", "Đã đổi mật khẩu. Vui lòng đăng nhập lại.");
    }

    @PostMapping("/logout")
    public ApiMessage logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        authService.logout(bearerToken(authorization));
        return new ApiMessage("LOGGED_OUT", "Đã đăng xuất.");
    }

    private String bearerToken(String authorization) {
        if (authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorization.substring(7).trim();
        }
        return "";
    }
}
