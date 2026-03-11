package kr.devport.api.domain.auth.service;

import kr.devport.api.domain.auth.entity.RefreshToken;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.dto.response.TokenResponse;
import kr.devport.api.domain.auth.dto.response.UserResponse;
import kr.devport.api.domain.auth.repository.UserRepository;
import kr.devport.api.domain.common.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
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
    private final OAuth2ExchangeCodeService oAuth2ExchangeCodeService;

    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        return convertToUserResponse(user);
    }

    @Transactional
    public TokenResponse refreshAccessToken(String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenService.requireValidRefreshToken(refreshTokenString);
        User user = refreshToken.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String rotatedRefreshToken = refreshTokenService.rotateRefreshToken(refreshToken);

        return TokenResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(rotatedRefreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
            .build();
    }

    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        refreshTokenService.deleteByUser(user);
    }

    @Transactional
    public TokenResponse exchangeOAuth2Code(String code, HttpServletRequest request) {
        User user = oAuth2ExchangeCodeService.consumeExchangeCode(code, request);
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return TokenResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
            .build();
    }

    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .username(user.getUsername())
            .name(user.getName())
            .profileImageUrl(user.getProfileImageUrl())
            .authProvider(user.getAuthProvider())
            .role(user.getRole())
            .emailVerified(user.getEmailVerified())
            .createdAt(user.getCreatedAt())
            .lastLoginAt(user.getLastLoginAt())
            .build();
    }
}
