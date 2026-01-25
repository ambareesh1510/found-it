package lost_and_found.app.auth.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import lost_and_found.app.exceptions.MalformedRegistrationRequestError;

/**
 * The single password rule for the app. Used by registration and password reset.
 * Keep the rules in sync with frontend/src/passwordPolicy.js.
 *
 * Rules: at least 12 characters, one uppercase letter, one lowercase letter,
 * and one special character (anything that is not a letter, digit, or whitespace).
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;

    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9\\s]");

    private PasswordPolicy() {
    }

    public static List<String> violations(String password) {
        List<String> problems = new ArrayList<>();
        if (password == null || password.length() < MIN_LENGTH) {
            problems.add("Password must be at least " + MIN_LENGTH + " characters long.");
        }
        if (password == null || !UPPER.matcher(password).find()) {
            problems.add("Password must include an uppercase letter.");
        }
        if (password == null || !LOWER.matcher(password).find()) {
            problems.add("Password must include a lowercase letter.");
        }
        if (password == null || !SPECIAL.matcher(password).find()) {
            problems.add("Password must include a special character, such as ! @ # $ %.");
        }
        return problems;
    }

    public static void requireValid(String password) {
        List<String> problems = violations(password);
        if (!problems.isEmpty()) {
            throw new MalformedRegistrationRequestError(String.join(" ", problems));
        }
    }
}
