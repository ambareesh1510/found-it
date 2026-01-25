package lost_and_found.app.exceptions;

public class UnsupportedOAuthTypeError extends RuntimeException {
    public UnsupportedOAuthTypeError(String message) {
        super(message);
    }
}
