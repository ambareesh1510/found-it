package lost_and_found.app.exceptions;

public class ClaimNotExistException extends RuntimeException {
    public ClaimNotExistException(String m) {
        super(m);
    }
}
