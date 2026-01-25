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

    public EmailServiceTest() {
        super(null, "http://localhost:5173");
    }

    @Override
    public void sendVerificationEmail(String to, String token) {
        verificationTokensByEmail.put(to, token);
    }

    @Override
    public void sendResetEmail(String to, String token) {
        resetTokensByEmail.put(to, token);
    }

    public String lastVerificationToken(String email) {
        return verificationTokensByEmail.get(email);
    }

    public String lastResetToken(String email) {
        return resetTokensByEmail.get(email);
    }
}
