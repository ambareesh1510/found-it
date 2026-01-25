package lost_and_found.app.exceptions;

public class ItemNotFoundException extends RuntimeException {
    public ItemNotFoundException(String m) {
        super(m);
    }
}
