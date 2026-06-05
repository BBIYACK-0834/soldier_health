package com.teukgeupjeonsa.backend.auth;

import com.teukgeupjeonsa.backend.user.ProfileImageStorageService;
import com.teukgeupjeonsa.backend.user.User;
import com.teukgeupjeonsa.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final com.teukgeupjeonsa.backend.common.security.JwtTokenProvider jwtTokenProvider;
    private final ProfileImageStorageService profileImageStorageService;

    @Transactional
    public SignupResponse signup(SignUpRequest request) {
        return signup(request, null);
    }

    @Transactional
    public SignupResponse signup(SignUpRequest request, MultipartFile profileImageFile) {
        validateSignupRequest(request);

        String email = request.getEmail().trim();
        String nickname = request.getNickname().trim();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        String profileImageUrl = profileImageFile != null && !profileImageFile.isEmpty()
                ? profileImageStorageService.store(profileImageFile)
                : normalizeProfileImageUrl(request.getProfileImageUrl());

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .build();

        User saved = userRepository.save(user);

        return SignupResponse.builder()
                .id(saved.getId())
                .email(saved.getEmail())
                .nickname(saved.getNickname())
                .profileImageUrl(saved.getProfileImageUrl())
                .build();
    }

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
            ));
        } catch (BadCredentialsException e) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail());

        return LoginResponse.builder()
                .accessToken(token)
                .user(AuthUserSummary.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .profileImageUrl(user.getProfileImageUrl())
                        .build())
                .build();
    }

    private void validateSignupRequest(SignUpRequest request) {
        if (request == null
                || request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()
                || request.getNickname() == null || request.getNickname().isBlank()) {
            throw new IllegalArgumentException("회원가입 정보를 모두 입력해주세요.");
        }
    }

    private String normalizeProfileImageUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
