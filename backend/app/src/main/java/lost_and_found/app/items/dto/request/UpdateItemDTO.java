package lost_and_found.app.items.dto.request;

import java.time.Instant;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record UpdateItemDTO(
    @NotNull(message = "Item ID is required.")
    Long itemId,
    String title,
    String description,
    String category,
    String imageUrl,
    String imageUrl2,
    @DecimalMin(value = "-90.0", message = "Latitude must be at least -90.")
    @DecimalMax(value = "90.0", message = "Latitude must be at most 90.")
    Double latitude,
    @DecimalMin(value = "-180.0", message = "Longitude must be at least -180.")
    @DecimalMax(value = "180.0", message = "Longitude must be at most 180.")
    Double longitude,
    String locationText,
    String detailLocation,
    Instant eventTime,
    String status,
    Long resolvedToUserId
) {
    private static final long EVENT_TIME_CLOCK_SKEW_SECONDS = 5;

    @AssertTrue(message = "Latitude and longitude must both be provided when updating location.")
    public boolean hasValidLocationPair() {
        return (latitude == null) == (longitude == null);
    }

    @AssertTrue(message = "Event time must be in the past or present.")
    public boolean isEventTimeValid() {
        return eventTime == null || !eventTime.isAfter(Instant.now().plusSeconds(EVENT_TIME_CLOCK_SKEW_SECONDS));
    }
}
