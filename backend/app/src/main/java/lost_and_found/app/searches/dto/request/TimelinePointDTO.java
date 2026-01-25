package lost_and_found.app.searches.dto.request;

import java.time.Instant;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** A device-local location sample submitted transiently with a Timeline search. */
public record TimelinePointDTO(
    @NotNull(message = "Timeline point latitude is required.")
    @DecimalMin(value = "-90.0", message = "Timeline point latitude must be at least -90.")
    @DecimalMax(value = "90.0", message = "Timeline point latitude must be at most 90.")
    Double latitude,

    @NotNull(message = "Timeline point longitude is required.")
    @DecimalMin(value = "-180.0", message = "Timeline point longitude must be at least -180.")
    @DecimalMax(value = "180.0", message = "Timeline point longitude must be at most 180.")
    Double longitude,

    @NotNull(message = "Timeline point time is required.")
    Instant recordedAt
) {}
