package com.teukgeupjeonsa.backend.auth;

import com.teukgeupjeonsa.backend.common.response.ApiResponse;
import com.teukgeupjeonsa.backend.user.User;
import com.teukgeupjeonsa.backend.user.UserProfileResponse;
import com.teukgeupjeonsa.backend.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/signup")
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignUpRequest request) {
        return ApiResponse.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return ApiResponse.ok(userService.getMyProfile(user.getId()));
    }
}
