package lost_and_found.app.exceptions;

/**
 * Thrown when an email-verification or password-reset token is unknown, already used, or expired.
 */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
