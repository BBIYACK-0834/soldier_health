package com.teukgeupjeonsa.backend.auth;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String accessToken;
    private Long userId;
    private String email;
    private String nickname;
}
