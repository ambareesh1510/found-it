package lost_and_found.app.exceptions;

public class MalformedRegistrationRequestError extends RuntimeException {
    public MalformedRegistrationRequestError(String m) {
        super(m);
    }
}
