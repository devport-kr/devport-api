package kr.devport.api.domain.auth.service;

import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.common.logging.LogSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.verification-url}")
    private String verificationUrlTemplate;

    @Value("${app.email.reset-password-url}")
    private String resetPasswordUrlTemplate;

    public void sendVerificationEmail(User user, String token) {
        try {
            String verificationUrl = verificationUrlTemplate.replace("{token}", token);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("DevPort - 이메일 인증");
            message.setText(
                "안녕하세요 " + (user.getName() != null ? user.getName() : user.getUsername()) + "님,\n\n" +
                "devport에 가입해주셔서 감사합니다!\n\n" +
                "아래 링크를 클릭하여 이메일 주소를 인증해주세요:\n" +
                verificationUrl + "\n\n" +
                "이 링크는 24시간 후에 만료됩니다.\n\n" +
                "본인이 가입하지 않았다면 이 이메일을 무시하셔도 됩니다.\n\n" +
                "감사합니다,\n" +
                "devport"
            );

            mailSender.send(message);
            log.debug("Verification email sent to {}", LogSanitizer.maskEmail(user.getEmail()));
        } catch (Exception e) {
            log.error("Failed to send verification email to {}", LogSanitizer.maskEmail(user.getEmail()), e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    public void sendPasswordResetEmail(User user, String token) {
        try {
            String resetUrl = resetPasswordUrlTemplate.replace("{token}", token);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("DevPort - 비밀번호 재설정");
            message.setText(
                "안녕하세요 " + (user.getName() != null ? user.getName() : user.getUsername()) + "님,\n\n" +
                "아래 링크를 클릭하여 비밀번호를 재설정해주세요:\n" +
                resetUrl + "\n\n" +
                "이 링크는 1시간 후에 만료됩니다.\n\n" +
                "본인이 요청하지 않았다면 이 이메일을 무시하셔도 됩니다.\n\n" +
                "감사합니다,\n" +
                "devport"
            );

            mailSender.send(message);
            log.debug("Password reset email sent to {}", LogSanitizer.maskEmail(user.getEmail()));
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}", LogSanitizer.maskEmail(user.getEmail()), e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
}
