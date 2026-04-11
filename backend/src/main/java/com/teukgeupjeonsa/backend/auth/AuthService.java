package com.teukgeupjeonsa.backend.auth;

import com.teukgeupjeonsa.backend.common.security.JwtTokenProvider;
import com.teukgeupjeonsa.backend.user.User;
import com.teukgeupjeonsa.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse signup(SignUpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .goalType(request.getGoalType())
                .workoutLevel(request.getWorkoutLevel())
                .branchType(request.getBranchType())
                .build();

        User saved = userRepository.save(user);
        String token = jwtTokenProvider.createToken(saved.getId(), saved.getEmail());

        return AuthResponse.builder()
                .accessToken(token)
                .userId(saved.getId())
                .email(saved.getEmail())
                .nickname(saved.getNickname())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
        ));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail());

        return AuthResponse.builder()
                .accessToken(token)
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }
}
