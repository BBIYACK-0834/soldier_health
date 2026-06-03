package com.teukgeupjeonsa.backend.auth;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignupResponse {
    private Long id;
    private String email;
    private String nickname;
}
