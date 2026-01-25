package lost_and_found.app.searches.dto.response;

import java.time.Instant;

/**
 * Non-identifying view of a FOUND post, shown to a loser in search results.
 * Deliberately omits detailLocation, imageUrl2, coordinates, and the poster's identity:
 * those are the finder's verification details (see docs/security.md).
 */
public record FoundItemSummaryDTO(
    Long itemId,
    String title,
    String description,
    String category,
    String imageUrl,
    String locationText,
    Instant eventTime
) {}
