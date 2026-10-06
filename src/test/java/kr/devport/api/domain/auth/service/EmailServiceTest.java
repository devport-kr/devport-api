package kr.devport.api.domain.auth.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.common.exception.EmailDeliveryException;
import kr.devport.api.domain.common.mail.MailTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
        emailService = new EmailService(mailSender, MailTestSupport.messageFactory(mailSender));
        ReflectionTestUtils.setField(emailService, "verificationUrlTemplate", "https://devport.kr/verify-email?token={token}");
        ReflectionTestUtils.setField(emailService, "resetPasswordUrlTemplate", "https://devport.kr/reset-password?token={token}");
    }

    private MimeMessage sentMessage() {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    @Test
    void verificationEmailIsRenderedWithLogo() throws Exception {
        emailService.sendVerificationEmail(User.builder().email("kim@example.com").username("kim").build(), "abc");

        MimeMessage message = sentMessage();
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("kim@example.com");
        assertThat(message.getSubject()).isEqualTo("[devport] 이메일 인증");
        assertThat(MailTestSupport.text(message))
            .startsWith("안녕하세요 kim님,")
            .contains("https://devport.kr/verify-email?token=abc");
        assertThat(MailTestSupport.html(message))
            .contains("href=\"https://devport.kr/verify-email?token=abc\"")
            .contains("src=\"cid:devport-logo\"")
            .doesNotContain("{{");
    }

    @Test
    void passwordResetEmailIsRenderedWithLogo() throws Exception {
        emailService.sendPasswordResetEmail(User.builder().email("kim@example.com").username("kim").name("김개발").build(), "xyz");

        MimeMessage message = sentMessage();
        assertThat(message.getSubject()).isEqualTo("[devport] 비밀번호 재설정");
        assertThat(MailTestSupport.text(message))
            .startsWith("안녕하세요 김개발님,")
            .contains("https://devport.kr/reset-password?token=xyz");
        assertThat(MailTestSupport.html(message))
            .contains("href=\"https://devport.kr/reset-password?token=xyz\"")
            .contains("src=\"cid:devport-logo\"")
            .doesNotContain("{{");
    }

    @Test
    void signupCodeEmailShowsCodeAndExpiry() throws Exception {
        emailService.sendSignupCode("kim@example.com", "042917", 10);

        MimeMessage message = sentMessage();
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("kim@example.com");
        assertThat(message.getSubject()).isEqualTo("[devport] 회원가입 인증번호");
        assertThat(MailTestSupport.text(message)).contains("인증번호: 042917").contains("10분 후에 만료").endsWith("감사합니다,\ndevport.kr\n");
        assertThat(MailTestSupport.html(message))
            .contains(">042917</span>")
            .contains("src=\"cid:devport-logo\"")
            .doesNotContain("{{");
    }

    @Test
    void signupCodeEmailFailureSurfacesAsEmailDeliveryException() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.sendSignupCode("kim@example.com", "042917", 10))
            .isInstanceOf(EmailDeliveryException.class);
    }

    @Test
    void sendFailureSurfacesAsRuntimeException() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.sendVerificationEmail(User.builder().email("kim@example.com").username("kim").build(), "abc"))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to send verification email");
    }
}
