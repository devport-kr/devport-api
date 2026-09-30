package kr.devport.api.domain.auth.service;

import kr.devport.api.domain.auth.entity.RefreshToken;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.repository.RefreshTokenRepository;
import kr.devport.api.domain.common.exception.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository);
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpirationMs", 2_592_000_000L);
    }

    @Test
    void createRefreshTokenStoresOnlyHashedToken() {
        User user = User.builder().id(1L).username("tester").build();
        when(refreshTokenRepository.save(any(RefreshToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        String rawToken = refreshTokenService.createRefreshToken(user);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken savedToken = captor.getValue();

        verify(refreshTokenRepository).deleteByUser(user);
        assertNotEquals(rawToken, savedToken.getToken());
    }

    @Test
    void requireValidRefreshTokenLooksUpHashedToken() {
        RefreshToken validToken = RefreshToken.builder()
            .user(User.builder().id(1L).build())
            .token("stored-hash")
            .expiresAt(LocalDateTime.now().plusMinutes(5))
            .createdAt(LocalDateTime.now())
            .build();
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(validToken));

        assertDoesNotThrow(() -> refreshTokenService.requireValidRefreshToken("plain-token"));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(refreshTokenRepository).findByToken(captor.capture());
        assertNotEquals("plain-token", captor.getValue());
    }

    @Test
    void requireValidRefreshTokenRejectsExpiredToken() {
        RefreshToken expiredToken = RefreshToken.builder()
            .user(User.builder().id(1L).build())
            .token("stored-hash")
            .expiresAt(LocalDateTime.now().minusMinutes(1))
            .createdAt(LocalDateTime.now().minusDays(1))
            .build();
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(expiredToken));

        assertThrows(InvalidTokenException.class,
            () -> refreshTokenService.requireValidRefreshToken("plain-token"));
        verify(refreshTokenRepository).delete(expiredToken);
    }
}
