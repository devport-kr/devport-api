package kr.devport.api.domain.auth.service;

import kr.devport.api.domain.auth.entity.EmailVerificationToken;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.enums.AuthProvider;
import kr.devport.api.domain.auth.dto.request.FlairUpdateRequest;
import kr.devport.api.domain.auth.dto.request.PasswordChangeRequest;
import kr.devport.api.domain.auth.dto.request.ProfileUpdateRequest;
import kr.devport.api.domain.common.exception.DuplicateEmailException;
import kr.devport.api.domain.common.exception.InvalidCredentialsException;
import kr.devport.api.domain.common.exception.OAuth2AccountException;
import kr.devport.api.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public User updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        boolean emailChanged = false;

        // Update email if provided and different
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            // Check email uniqueness
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateEmailException("Email already exists: " + request.getEmail());
            }

            user.setEmail(request.getEmail());
            user.setEmailVerified(false);
            user.setEmailAddedAt(LocalDateTime.now());
            emailChanged = true;
        }

        // Update name if provided
        if (request.getName() != null) {
            user.setName(request.getName());
        }

        // Update profile image if provided
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }

        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        // Send verification email if email was changed
        if (emailChanged) {
            EmailVerificationToken token = emailVerificationService.createVerificationToken(user);
            emailService.sendVerificationEmail(user, token.getToken());
            log.info("Email changed for user: {}, verification email sent", user.getUsername());
        }

        log.info("Profile updated for user: {}", user.getUsername());
        return user;
    }

    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Only LOCAL users can change password
        if (user.getAuthProvider() != AuthProvider.local) {
            throw new OAuth2AccountException(
                "This account uses " + user.getAuthProvider().name() + " login. Password change is not available."
            );
        }

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        refreshTokenService.deleteByUser(user);

        log.info("Password changed for user: {}", user.getUsername());
    }

    @Transactional
    public void removeEmail(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Only OAuth users can remove email (LOCAL users need email for password reset)
        if (user.getAuthProvider() == AuthProvider.local) {
            throw new OAuth2AccountException("LOCAL users cannot remove email as it's required for password reset");
        }

        user.setEmail(null);
        user.setEmailVerified(false);
        user.setEmailAddedAt(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Email removed for user: {}", user.getId());
    }

    @Transactional
    public User updateFlair(Long userId, FlairUpdateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFlair(request.getFlair());
        user.setFlairColor(request.getFlairColor());
        user.setUpdatedAt(LocalDateTime.now());

        user = userRepository.save(user);
        log.info("Flair updated for user: {}", user.getUsername());
        return user;
    }
}
