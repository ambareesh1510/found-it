package lost_and_found.app.shared.util;

import java.util.Arrays;
import java.util.List;

/** Splits comma-separated configuration values, ignoring blanks. */
public final class Csv {

    private Csv() {}

    public static List<String> split(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }
}
