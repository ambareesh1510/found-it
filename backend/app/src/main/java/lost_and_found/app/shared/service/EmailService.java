package lost_and_found.app.shared.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;

/**
 * Sends the account emails through Brevo's HTTPS API. Links point at the frontend, which then calls the API.
 *
 * Hosts such as Render's free web services block outbound SMTP, so Gmail SMTP cannot be used there.
 * Brevo sends to any recipient once the sender address is verified in Brevo (no domain needed).
 */
@Service
public class EmailService {

    private static final URI BREVO_EMAILS_URI = URI.create("https://api.brevo.com/v3/smtp/email");
    private static final String DEFAULT_SENDER_NAME = "GT Lost and Found";
    private static final Pattern NAME_AND_ADDRESS = Pattern.compile("^\\s*(.*?)\\s*<([^<>]+)>\\s*$");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final String frontendUrl;
    private final String brevoApiKey;
    private final String senderName;
    private final String senderEmail;

    public EmailService(
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${BREVO_API_KEY:}") String brevoApiKey,
            @Value("${EMAIL_FROM:}") String fromAddress
    ) {
        this.frontendUrl = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
        this.brevoApiKey = brevoApiKey == null ? "" : brevoApiKey.trim();

        // Accepts "Name <address@example.com>" or a bare address.
        String from = fromAddress == null ? "" : fromAddress.trim();
        Matcher matcher = NAME_AND_ADDRESS.matcher(from);
        if (matcher.matches()) {
            this.senderName = matcher.group(1).isEmpty() ? DEFAULT_SENDER_NAME : matcher.group(1);
            this.senderEmail = matcher.group(2).trim();
        } else {
            this.senderName = DEFAULT_SENDER_NAME;
            this.senderEmail = from;
        }
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
        if (brevoApiKey.isEmpty()) {
            throw new MessagingException("BREVO_API_KEY is not set");
        }
        if (senderEmail.isEmpty()) {
            throw new MessagingException("EMAIL_FROM is not set");
        }

        String body = "{"
                + "\"sender\":{\"name\":" + json(senderName) + ",\"email\":" + json(senderEmail) + "},"
                + "\"to\":[{\"email\":" + json(to) + "}],"
                + "\"subject\":" + json(subject) + ","
                + "\"htmlContent\":" + json(html)
                + "}";

        HttpRequest request = HttpRequest.newBuilder(BREVO_EMAILS_URI)
                .timeout(Duration.ofSeconds(15))
                .header("api-key", brevoApiKey)
                .header("accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new MessagingException("Brevo returned HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (IOException e) {
            throw new MessagingException("Brevo request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MessagingException("Brevo request was interrupted", e);
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
