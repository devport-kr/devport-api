package kr.devport.api.domain.auth.service;

import jakarta.mail.MessagingException;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.common.exception.EmailDeliveryException;
import kr.devport.api.domain.common.logging.LogSanitizer;
import kr.devport.api.domain.common.mail.MailMessageFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/** 계정 메일 발송. 본문은 classpath:templates/mail/auth/ 의 템플릿을 쓴다 ({@link MailMessageFactory}). */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final MailMessageFactory messages;

    @Value("${app.email.verification-url}")
    private String verificationUrlTemplate;

    @Value("${app.email.reset-password-url}")
    private String resetPasswordUrlTemplate;

    public void sendVerificationEmail(User user, String token) {
        try {
            mailSender.send(messages.create(user.getEmail(), "[devport] 이메일 인증", "auth/verify-email", Map.of(
                "name", displayName(user),
                "verificationUrl", verificationUrlTemplate.replace("{token}", token))));
            log.debug("Verification email sent to {}", LogSanitizer.maskEmail(user.getEmail()));
        } catch (Exception e) {
            log.error("Failed to send verification email to {}", LogSanitizer.maskEmail(user.getEmail()), e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    public void sendPasswordResetEmail(User user, String token) {
        try {
            mailSender.send(messages.create(user.getEmail(), "[devport] 비밀번호 재설정", "auth/reset-password", Map.of(
                "name", displayName(user),
                "resetUrl", resetPasswordUrlTemplate.replace("{token}", token))));
            log.debug("Password reset email sent to {}", LogSanitizer.maskEmail(user.getEmail()));
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}", LogSanitizer.maskEmail(user.getEmail()), e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    /** 회원가입 이메일 인증번호. 실패하면 {@link EmailDeliveryException}(503)으로 알려 사용자가 다시 시도하게 한다. */
    public void sendSignupCode(String email, String code, long expiresInMinutes) {
        try {
            mailSender.send(messages.create(email, "[devport] 회원가입 인증번호", "auth/signup-code", Map.of(
                "code", code,
                "expiresInMinutes", String.valueOf(expiresInMinutes))));
            log.debug("Signup code email sent to {}", LogSanitizer.maskEmail(email));
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            throw new EmailDeliveryException("인증 메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.", e);
        }
    }

    private static String displayName(User user) {
        return user.getName() != null ? user.getName() : user.getUsername();
    }
}
