package kr.devport.api.service;

import kr.devport.api.domain.entity.RefreshToken;
import kr.devport.api.domain.entity.User;
import kr.devport.api.dto.response.TokenResponse;
import kr.devport.api.dto.response.UserResponse;
import kr.devport.api.repository.UserRepository;
import kr.devport.api.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;

    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        return convertToUserResponse(user);
    }

    @Transactional
    public TokenResponse refreshAccessToken(String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenString)
            .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (!refreshTokenService.verifyRefreshToken(refreshToken)) {
            throw new RuntimeException("Refresh token is expired or revoked");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId());

        return TokenResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(refreshTokenString)
            .tokenType("Bearer")
            .expiresIn(3600L) // 만료 시간 1시간(초 단위)
            .build();
    }

    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        refreshTokenService.deleteByUser(user);
    }

    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .name(user.getName())
            .profileImageUrl(user.getProfileImageUrl())
            .authProvider(user.getAuthProvider())
            .role(user.getRole())
            .createdAt(user.getCreatedAt())
            .lastLoginAt(user.getLastLoginAt())
            .build();
    }
}
