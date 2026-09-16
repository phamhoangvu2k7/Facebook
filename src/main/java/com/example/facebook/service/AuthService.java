package com.example.facebook.service;

import com.example.facebook.config.JwtUtils;
import com.example.facebook.dto.request.LoginRequest;
import com.example.facebook.dto.request.RefreshTokenRequest;
import com.example.facebook.dto.request.RegisterRequest;
import com.example.facebook.dto.response.AuthResponse;
import com.example.facebook.dto.response.UserResponse;
import com.example.facebook.entity.RefreshToken;
import com.example.facebook.entity.User;
import com.example.facebook.repository.RefreshTokenRepository;
import com.example.facebook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    // Register
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException("Email này đã được sử dụng!");

        String encodePassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .email(request.getEmail())
                .password(encodePassword)
                .fullName(request.getFullName())
                .build();

        User savedUser = userRepository.save(user);

        return UserResponse.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .avatarUrl(savedUser.getAvatarUrl())
                .bio(savedUser.getBio())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    // Login
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email hoặc mật khẩu không chính xác!"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new RuntimeException("Email hoặc mật khẩu không chính xác!");

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .createdAt(user.getCreatedAt())
                .build();

        String accessToken = jwtUtils.generateAccessToken(user);
        String refreshTokenStr = jwtUtils.generateRefreshToken(user);

        saveRefreshToken(user, refreshTokenStr);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .user(userResponse)
                .build();
    }

    private void saveRefreshToken(User user, String tokenStr) {
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUser(user);
        RefreshToken refreshToken;
        if (existingToken.isPresent()) {
            refreshToken = existingToken.get();
        } else {
            refreshToken = new RefreshToken();
            refreshToken.setUser(user);
        }
        refreshToken.setToken(tokenStr);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpiration));
        refreshTokenRepository.save(refreshToken);
    }

    // Refresh Token API handler
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Refresh Token không hợp lệ hoặc không tồn tại trong hệ thống!"));

        if (refreshToken.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh Token đã hết hạn! Vui lòng đăng nhập lại.");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtUtils.generateAccessToken(user);

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .createdAt(user.getCreatedAt())
                .build();

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken.getToken())
                .user(userResponse)
                .build();
    }
}
