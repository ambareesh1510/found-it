package lost_and_found.app.items.dto.response;

import java.time.Instant;
import java.util.List;

import lost_and_found.app.items.domain.ItemPostStatus;
import lost_and_found.app.items.domain.PostType;

/**
 * Item post as returned to clients.
 * {@code detailLocation} is non-null only when the viewer is the poster (see ItemMapper).
 * {@code rewardCents} is null when no reward is offered.
 */
public record ItemPostResponseDTO(
    Long itemId,
    Long posterId,
    String posterUsername,
    String title,
    String description,
    String category,
    ItemPostStatus status,
    String imageUrl,
    String imageUrl2,
    Double latitude,
    Double longitude,
    String locationText,
    String detailLocation,
    Instant eventTime,
    Instant createdAt,
    Instant updatedAt,
    List<String> tags,
    PostType postType,
    Integer rewardCents,
    /** The chosen counterparty for a resolution in progress/complete, or null. See ItemPost. */
    Long resolvedWithUserId,
    boolean posterConfirmedResolution,
    boolean counterpartyConfirmedResolution
) {

    /** Keeps the pre-resolution call sites (and tests) compiling. */
    public ItemPostResponseDTO(
        Long itemId,
        Long posterId,
        String posterUsername,
        String title,
        String description,
        String category,
        ItemPostStatus status,
        String imageUrl,
        String imageUrl2,
        Double latitude,
        Double longitude,
        String locationText,
        String detailLocation,
        Instant eventTime,
        Instant createdAt,
        Instant updatedAt,
        List<String> tags,
        PostType postType,
        Integer rewardCents
    ) {
        this(itemId, posterId, posterUsername, title, description, category, status,
            imageUrl, imageUrl2, latitude, longitude, locationText, detailLocation,
            eventTime, createdAt, updatedAt, tags, postType, rewardCents, null, false, false);
    }

    /** Keeps the pre-reward call sites (and tests) compiling. */
    public ItemPostResponseDTO(
        Long itemId,
        Long posterId,
        String posterUsername,
        String title,
        String description,
        String category,
        ItemPostStatus status,
        String imageUrl,
        String imageUrl2,
        Double latitude,
        Double longitude,
        String locationText,
        String detailLocation,
        Instant eventTime,
        Instant createdAt,
        Instant updatedAt,
        List<String> tags,
        PostType postType
    ) {
        this(itemId, posterId, posterUsername, title, description, category, status,
            imageUrl, imageUrl2, latitude, longitude, locationText, detailLocation,
            eventTime, createdAt, updatedAt, tags, postType, null);
    }

    /** Keeps the pre-postType call sites (and tests) compiling. */
    public ItemPostResponseDTO(
        Long itemId,
        Long posterId,
        String posterUsername,
        String title,
        String description,
        String category,
        ItemPostStatus status,
        String imageUrl,
        String imageUrl2,
        Double latitude,
        Double longitude,
        String locationText,
        String detailLocation,
        Instant eventTime,
        Instant createdAt,
        Instant updatedAt,
        List<String> tags
    ) {
        this(itemId, posterId, posterUsername, title, description, category, status,
            imageUrl, imageUrl2, latitude, longitude, locationText, detailLocation,
            eventTime, createdAt, updatedAt, tags, null, null);
    }
}
