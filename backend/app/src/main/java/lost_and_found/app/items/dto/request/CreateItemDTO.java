package lost_and_found.app.items.dto.request;

import java.time.Instant;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lost_and_found.app.items.domain.PostType;

public record CreateItemDTO(
    @NotBlank(message = "Title is required.")
    String title,
    String description,
    @NotBlank(message = "Category is required.")
    String category,
    /**
     * Optional. Blank or null means no photo. When given it is an http(s) link or an inline
     * base64 image (the create page uploads files as data URLs). Other schemes are rejected.
     */
    @Pattern(
        regexp = "^(https?://\\S+|data:image/[A-Za-z0-9.+-]+;base64,\\S+)?$",
        message = "Photo must be an http(s) link or an image file."
    )
    String imageUrl,
    String imageUrl2,
    @NotNull(message = "Latitude is required.")
    @DecimalMin(value = "-90.0", message = "Latitude must be at least -90.")
    @DecimalMax(value = "90.0", message = "Latitude must be at most 90.")
    Double latitude,
    @NotNull(message = "Longitude is required.")
    @DecimalMin(value = "-180.0", message = "Longitude must be at least -180.")
    @DecimalMax(value = "180.0", message = "Longitude must be at most 180.")
    Double longitude,
    String locationText,
    /** Shown only to the poster (see ItemMapper). */
    String detailLocation,
    @NotNull(message = "Event time is required.")
    Instant eventTime,
    /** LOST or FOUND. Omitted means LOST, matching the entity default. */
    PostType postType,
    /** Optional reward in cents, LOST posts only. */
    @PositiveOrZero(message = "Reward cannot be negative.")
    @Max(value = 10_000_000, message = "Reward cannot be more than $100,000.")
    Integer rewardCents
) {
    private static final long EVENT_TIME_CLOCK_SKEW_SECONDS = 5;

    /** Keeps the pre-postType call sites (and tests) compiling. */
    public CreateItemDTO(
        String title,
        String description,
        String category,
        String imageUrl,
        String imageUrl2,
        Double latitude,
        Double longitude,
        String locationText,
        String detailLocation,
        Instant eventTime
    ) {
        this(title, description, category, imageUrl, imageUrl2, latitude, longitude,
            locationText, detailLocation, eventTime, null, null);
    }

    /** Keeps the pre-reward call sites compiling. */
    public CreateItemDTO(
        String title,
        String description,
        String category,
        String imageUrl,
        String imageUrl2,
        Double latitude,
        Double longitude,
        String locationText,
        String detailLocation,
        Instant eventTime,
        PostType postType
    ) {
        this(title, description, category, imageUrl, imageUrl2, latitude, longitude,
            locationText, detailLocation, eventTime, postType, null);
    }

    @AssertTrue(message = "Latitude and longitude are required together.")
    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    @AssertTrue(message = "Event time must be in the past or present.")
    public boolean isEventTimeValid() {
        return eventTime != null && !eventTime.isAfter(Instant.now().plusSeconds(EVENT_TIME_CLOCK_SKEW_SECONDS));
    }

    @AssertTrue(message = "Reward applies only to lost reports.")
    public boolean isRewardAllowed() {
        return rewardCents == null || postType == null || postType == PostType.LOST;
    }
}
