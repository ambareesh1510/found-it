package lost_and_found.app.test_implementations;

import java.util.HashMap;
import java.util.Map;

import lost_and_found.app.shared.service.EmailService;

/**
 * Records the tokens that would have been emailed, instead of contacting an SMTP server.
 */
public class EmailServiceTest extends EmailService {

    private final Map<String, String> verificationTokensByEmail = new HashMap<>();
    private final Map<String, String> resetTokensByEmail = new HashMap<>();
    private final Map<String, String> foundMatchTitlesByEmail = new HashMap<>();
    private final java.util.Set<String> failForRecipients = new java.util.HashSet<>();

    public EmailServiceTest() {
        super("http://localhost:5173", "", "test@example.com");
    }

    @Override
    public void sendVerificationEmail(String to, String token) {
        verificationTokensByEmail.put(to, token);
    }

    @Override
    public void sendResetEmail(String to, String token) {
        resetTokensByEmail.put(to, token);
    }

    @Override
    public void sendFoundMatchEmail(String to, String lostReportTitle) throws jakarta.mail.MessagingException {
        if (failForRecipients.contains(to)) {
            throw new jakarta.mail.MessagingException("simulated send failure for " + to);
        }
        foundMatchTitlesByEmail.put(to, lostReportTitle);
    }

    /** Make the next send to {@code email} throw, to test best-effort delivery. */
    public void failSendsTo(String email) {
        failForRecipients.add(email);
    }

    /** Recipients that actually received a found-match email. */
    public java.util.Set<String> foundMatchRecipients() {
        return foundMatchTitlesByEmail.keySet();
    }

    public String lastFoundMatchTitle(String email) {
        return foundMatchTitlesByEmail.get(email);
    }

    public String lastVerificationToken(String email) {
        return verificationTokensByEmail.get(email);
    }

    public String lastResetToken(String email) {
        return resetTokensByEmail.get(email);
    }
}
