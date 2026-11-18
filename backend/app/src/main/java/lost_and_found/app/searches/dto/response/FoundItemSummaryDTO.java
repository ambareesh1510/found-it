package lost_and_found.app.searches.dto.response;

import java.time.Instant;

/**
 * Non-identifying view of a FOUND post, shown to a loser in search results.
 * Deliberately omits detailLocation, imageUrl2, exact coordinates, and the poster's identity.
 * Rounded coordinates are included only so the declaration-gated results can be mapped.
 */
public record FoundItemSummaryDTO(
    Long itemId,
    String title,
    String description,
    String category,
    String imageUrl,
    String locationText,
    Double latitude,
    Double longitude,
    Instant eventTime
) {}
