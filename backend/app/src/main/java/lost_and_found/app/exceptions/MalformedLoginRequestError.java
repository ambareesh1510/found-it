package lost_and_found.app.exceptions;

public class MalformedLoginRequestError extends RuntimeException {
    public MalformedLoginRequestError(String m) {
        super(m);
    }
}
