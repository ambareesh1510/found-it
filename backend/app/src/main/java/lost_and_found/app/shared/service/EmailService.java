package lost_and_found.app.shared.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;

/**
 * Sends the account emails through Resend's HTTPS API. Links point at the frontend, which then calls the API.
 *
 * Hosts such as Render's free web services block outbound SMTP, so Gmail SMTP cannot be used there.
 */
@Service
public class EmailService {

    private static final URI RESEND_EMAILS_URI = URI.create("https://api.resend.com/emails");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final String frontendUrl;
    private final String resendApiKey;
    private final String fromAddress;

    public EmailService(
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${RESEND_API_KEY:}") String resendApiKey,
            @Value("${EMAIL_FROM:GT Lost and Found <onboarding@resend.dev>}") String fromAddress
    ) {
        this.frontendUrl = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
        this.resendApiKey = resendApiKey == null ? "" : resendApiKey.trim();
        this.fromAddress = fromAddress;
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
        if (resendApiKey.isEmpty()) {
            throw new MessagingException("RESEND_API_KEY is not set");
        }

        String body = "{"
                + "\"from\":" + json(fromAddress) + ","
                + "\"to\":[" + json(to) + "],"
                + "\"subject\":" + json(subject) + ","
                + "\"html\":" + json(html)
                + "}";

        HttpRequest request = HttpRequest.newBuilder(RESEND_EMAILS_URI)
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new MessagingException("Resend returned HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (IOException e) {
            throw new MessagingException("Resend request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MessagingException("Resend request was interrupted", e);
        }
    }

    /** Encodes a string as a JSON string literal. */
    private static String json(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
