package kr.devport.api.domain.common.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimePart;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** 메일 테스트용: 실제 직렬화 형태로 다시 읽어서 text/html/inline 파트를 꺼낸다. */
public final class MailTestSupport {

    private MailTestSupport() {
    }

    public static MailMessageFactory messageFactory(JavaMailSender mailSender) {
        MailMessageFactory factory = new MailMessageFactory(mailSender, new MailTemplateRenderer());
        ReflectionTestUtils.setField(factory, "fromEmail", "noreply@devport.kr");
        ReflectionTestUtils.setField(factory, "senderName", "devport");
        ReflectionTestUtils.setField(factory, "siteUrl", "https://devport.kr");
        return factory;
    }

    public static String text(MimeMessage message) throws MessagingException, IOException {
        return (String) part(message, "text/plain").getContent();
    }

    public static String html(MimeMessage message) throws MessagingException, IOException {
        return (String) part(message, "text/html").getContent();
    }

    public static MimePart inline(MimeMessage message, String contentId) throws MessagingException, IOException {
        for (Part leaf : leaves(message)) {
            if (leaf instanceof MimePart mimePart && ("<" + contentId + ">").equals(mimePart.getContentID())) {
                return mimePart;
            }
        }
        throw new AssertionError("No inline part with Content-ID <" + contentId + ">");
    }

    private static Part part(MimeMessage message, String mimeType) throws MessagingException, IOException {
        for (Part leaf : leaves(message)) {
            if (leaf.isMimeType(mimeType)) {
                return leaf;
            }
        }
        throw new AssertionError("No " + mimeType + " part");
    }

    private static List<Part> leaves(MimeMessage message) throws MessagingException, IOException {
        message.saveChanges();
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        message.writeTo(raw);
        MimeMessage parsed = new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(raw.toByteArray()));

        List<Part> leaves = new ArrayList<>();
        collect(parsed, leaves);
        return leaves;
    }

    private static void collect(Part part, List<Part> leaves) throws MessagingException, IOException {
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                collect(multipart.getBodyPart(i), leaves);
            }
        } else {
            leaves.add(part);
        }
    }
}
