package lost_and_found.app.shared.domain;

import java.time.Instant;

public record ApiError(
    Instant timestamp,
    int status,
    String error,
    String message
) {}