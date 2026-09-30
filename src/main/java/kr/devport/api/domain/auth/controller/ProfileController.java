package kr.devport.api.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.dto.request.FlairUpdateRequest;
import kr.devport.api.domain.auth.dto.request.PasswordChangeRequest;
import kr.devport.api.domain.auth.dto.request.ProfileUpdateRequest;
import kr.devport.api.domain.auth.dto.response.UserResponse;
import kr.devport.api.domain.common.security.CustomUserDetails;
import kr.devport.api.domain.auth.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Profile", description = "User profile management endpoints")
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;

    @Operation(
        summary = "Update profile",
        description = "Update user profile (email, name, profile image). Email changes require re-verification."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile updated successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "409", description = "Email already exists", content = @Content)
    })
    @PutMapping
    public ResponseEntity<UserResponse> updateProfile(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody ProfileUpdateRequest request
    ) {
        User user = profileService.updateProfile(userDetails.getId(), request);

        UserResponse response = UserResponse.builder()
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
            .flair(user.getFlair())
            .flairColor(user.getFlairColor())
            .build();

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Change password",
        description = "Change password for LOCAL users only"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password changed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or OAuth account", content = @Content),
        @ApiResponse(responseCode = "401", description = "Current password incorrect", content = @Content)
    })
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody PasswordChangeRequest request
    ) {
        profileService.changePassword(userDetails.getId(), request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @Operation(
        summary = "Remove email",
        description = "Remove email address (OAuth users only, not allowed for LOCAL users)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Email removed successfully"),
        @ApiResponse(responseCode = "400", description = "LOCAL users cannot remove email", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    @DeleteMapping("/email")
    public ResponseEntity<Map<String, String>> removeEmail(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        profileService.removeEmail(userDetails.getId());
        return ResponseEntity.ok(Map.of("message", "Email removed successfully"));
    }

    @Operation(
        summary = "Update user flair",
        description = "Update user flair text and color (displayed in comments)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Flair updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid flair data", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    @PutMapping("/flair")
    public ResponseEntity<Map<String, String>> updateFlair(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody FlairUpdateRequest request
    ) {
        User user = profileService.updateFlair(userDetails.getId(), request);
        return ResponseEntity.ok(Map.of(
            "flair", user.getFlair() != null ? user.getFlair() : "",
            "flairColor", user.getFlairColor() != null ? user.getFlairColor() : ""
        ));
    }
}
