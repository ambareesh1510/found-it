package lost_and_found.app.auth.dto;

import java.util.Locale;

/** Shared input cleanup for auth DTOs. */
final class Normalize {

    private Normalize() {
    }

    /** Trims and lowercases a value, keeping null as null so bean validation can still report it. */
    static String lower(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
