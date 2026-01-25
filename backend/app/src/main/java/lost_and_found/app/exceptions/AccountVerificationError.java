package lost_and_found.app.exceptions;

public class AccountVerificationError extends RuntimeException {
    public AccountVerificationError(String m) {
        super(m);
    }
}
