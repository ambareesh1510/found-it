package lost_and_found.app.shared.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Sends the account emails. Links point at the frontend, which then calls the API.
 */
@Service
public class EmailService {

    private final JavaMailSender emailSender;
    private final String frontendUrl;

    public EmailService(
            JavaMailSender emailSender,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.emailSender = emailSender;
        this.frontendUrl = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
    }

    public void sendVerificationEmail(String to, String token) throws MessagingException {
        String link = frontendUrl + "/verify-email?token=" + token;
        String html = """
        <p>Welcome to GT Lost and Found.</p>
        <p>Confirm your email address to activate your account:</p>
        <p><a href="%s">Verify my email</a></p>
        <p>This link expires in 24 hours. If you did not create an account, you can ignore this email.</p>
        """.formatted(link);
        send(to, "Verify your GT Lost and Found account", html);
    }

    public void sendResetEmail(String to, String token) throws MessagingException {
        String link = frontendUrl + "/reset-password/" + token;
        String html = """
        <p>You requested a password reset.</p>
        <p>Click the link below to choose a new password:</p>
        <p><a href="%s">Reset password</a></p>
        <p>This link expires in 1 hour. If you did not request this, you can ignore this email.</p>
        """.formatted(link);
        send(to, "Reset your password", html);
    }

    private void send(String to, String subject, String html) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        emailSender.send(message);
    }
}
