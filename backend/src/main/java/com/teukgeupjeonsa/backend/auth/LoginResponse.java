package com.teukgeupjeonsa.backend.auth;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
    private String accessToken;
    private AuthUserSummary user;
}
