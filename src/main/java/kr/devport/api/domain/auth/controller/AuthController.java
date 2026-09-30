package kr.devport.api.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.devport.api.domain.auth.dto.request.ForgotPasswordRequest;
import kr.devport.api.domain.auth.dto.request.LoginRequest;
import kr.devport.api.domain.auth.dto.request.OAuth2ExchangeRequest;
import kr.devport.api.domain.auth.dto.request.ResetPasswordRequest;
import kr.devport.api.domain.auth.dto.request.ResendVerificationRequest;
import kr.devport.api.domain.auth.dto.request.SignupRequest;
import kr.devport.api.domain.auth.dto.response.AuthResponse;
import kr.devport.api.domain.auth.dto.response.SignupResponse;
import kr.devport.api.domain.auth.dto.response.TokenResponse;
import kr.devport.api.domain.auth.dto.response.UserResponse;
import kr.devport.api.domain.common.security.CustomUserDetails;
import kr.devport.api.domain.auth.service.AuthService;
import kr.devport.api.domain.auth.service.EmailVerificationService;
import kr.devport.api.domain.auth.service.LoginService;
import kr.devport.api.domain.auth.service.PasswordResetService;
import kr.devport.api.domain.auth.service.RefreshTokenCookieService;
import kr.devport.api.domain.auth.service.SignupService;
import kr.devport.api.domain.common.exception.InvalidTokenException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@Tag(name = "Authentication", description = "Authentication and user management endpoints")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AuthController {

    private final AuthService authService;
    private final SignupService signupService;
    private final LoginService loginService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    @Operation(
        summary = "Get current user",
        description = "Retrieve information about the currently authenticated user. Requires a valid JWT token."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved user information",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing JWT token",
            content = @Content
        )
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UserResponse user = authService.getCurrentUser(userDetails.getId());
        return ResponseEntity.ok(user);
    }

    @Operation(
        summary = "Refresh access token",
        description = "Get a new access token using a valid refresh token. Does not require authentication."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully refreshed access token",
            content = @Content(schema = @Schema(implementation = TokenResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid or expired refresh token",
            content = @Content
        )
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshAccessToken(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        String refreshToken = requireRefreshToken(resolveRefreshToken(request));
        TokenResponse tokenResponse = authService.refreshAccessToken(requireRefreshToken(refreshToken));
        refreshTokenCookieService.addRefreshTokenCookie(response, tokenResponse.getRefreshToken());
        return ResponseEntity.ok(stripRefreshToken(tokenResponse));
    }

    @Operation(
        summary = "Logout",
        description = "Revoke all refresh tokens for the current user. Requires a valid JWT token."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully logged out"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing JWT token",
            content = @Content
        )
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        HttpServletResponse response
    ) {
        authService.logout(userDetails.getId());
        refreshTokenCookieService.clearRefreshTokenCookie(response);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Sign up with username and password",
        description = "Create a new LOCAL account with username, password, and email. Sends verification email."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully created account",
            content = @Content(schema = @Schema(implementation = SignupResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Username or email already exists",
            content = @Content
        )
    })
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = signupService.signup(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Login with username and password",
        description = "Authenticate with username and password for LOCAL accounts"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully authenticated",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials",
            content = @Content
        )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletResponse servletResponse
    ) {
        AuthResponse authResponse = loginService.login(request);
        refreshTokenCookieService.addRefreshTokenCookie(servletResponse, authResponse.getRefreshToken());
        return ResponseEntity.ok(stripRefreshToken(authResponse));
    }

    @Operation(
        summary = "Exchange OAuth2 callback code",
        description = "Exchange a one-time OAuth2 callback code for an access token and refresh-token cookie."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully exchanged OAuth2 callback code",
            content = @Content(schema = @Schema(implementation = TokenResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid or expired exchange code",
            content = @Content
        )
    })
    @PostMapping("/oauth2/exchange")
    public ResponseEntity<TokenResponse> exchangeOAuth2Code(
        @Valid @RequestBody OAuth2ExchangeRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse response
    ) {
        TokenResponse tokenResponse = authService.exchangeOAuth2Code(request.getCode(), httpRequest);
        refreshTokenCookieService.addRefreshTokenCookie(response, tokenResponse.getRefreshToken());
        return ResponseEntity.ok(stripRefreshToken(tokenResponse));
    }

    @Operation(
        summary = "Verify email address",
        description = "Verify email address with token from email link"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Email verified successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired token", content = @Content),
        @ApiResponse(responseCode = "404", description = "Token not found", content = @Content)
    })
    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @Operation(
        summary = "Resend verification email",
        description = "Resend email verification link to the current user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verification email sent if the account is eligible")
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(
        @Valid @RequestBody ResendVerificationRequest request
    ) {
        emailVerificationService.resendVerificationEmailIfEligible(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "If the account is eligible, a verification email will be sent."));
    }

    @Operation(
        summary = "Request password reset",
        description = "Request password reset link via email (LOCAL accounts only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset email sent if the account is eligible")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.createResetToken(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "If the account is eligible, password reset instructions will be sent."));
    }

    @Operation(
        summary = "Reset password",
        description = "Reset password with token from email link"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired token", content = @Content),
        @ApiResponse(responseCode = "404", description = "Token not found", content = @Content)
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    private AuthResponse stripRefreshToken(AuthResponse response) {
        return AuthResponse.builder()
            .accessToken(response.getAccessToken())
            .tokenType(response.getTokenType())
            .expiresIn(response.getExpiresIn())
            .user(response.getUser())
            .build();
    }

    private TokenResponse stripRefreshToken(TokenResponse response) {
        return TokenResponse.builder()
            .accessToken(response.getAccessToken())
            .tokenType(response.getTokenType())
            .expiresIn(response.getExpiresIn())
            .build();
    }

    private String requireRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidTokenException("Refresh token is invalid or expired");
        }
        return refreshToken;
    }

    private String resolveRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (refreshTokenCookieService.getCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
