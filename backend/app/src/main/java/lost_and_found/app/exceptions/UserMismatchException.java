package lost_and_found.app.exceptions;

public class UserMismatchException extends RuntimeException {
    public UserMismatchException(String message) { 
        super(message);
    }
}
