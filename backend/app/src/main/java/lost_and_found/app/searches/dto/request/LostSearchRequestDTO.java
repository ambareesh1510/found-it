package lost_and_found.app.searches.dto.request;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The loser's declaration: category, where, and when. Sent in one call together with the
 * request for matching results, so results cannot be obtained without the record.
 */
public record LostSearchRequestDTO(
    @NotBlank(message = "Category is required.")
    String category,

    Double latitude,

    Double longitude,

    Double radiusMeters,

    Instant windowStart,

    Instant windowEnd,

    boolean useTimeline,

    @Valid
    @Size(max = 300, message = "Timeline may contain at most 300 points.")
    List<TimelinePointDTO> timelinePoints
) {

    @AssertTrue(message = "Manual searches require valid coordinates, radius, and an ordered time window.")
    public boolean isManualDeclarationValid() {
        if (useTimeline) return true;
        return latitude != null && latitude >= -90 && latitude <= 90
            && longitude != null && longitude >= -180 && longitude <= 180
            && radiusMeters != null && radiusMeters > 0 && radiusMeters <= 50000
            && windowStart != null && windowEnd != null && windowStart.isBefore(windowEnd);
    }

    @AssertTrue(message = "Timeline searches require at least one location point.")
    public boolean isTimelineDeclarationValid() {
        return !useTimeline || (timelinePoints != null && !timelinePoints.isEmpty());
    }
}
