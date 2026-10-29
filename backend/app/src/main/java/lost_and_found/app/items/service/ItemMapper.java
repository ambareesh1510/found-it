package lost_and_found.app.items.service;

import org.springframework.stereotype.Component;

import lost_and_found.app.items.domain.ItemPost;
import lost_and_found.app.items.dto.response.ItemPostResponseDTO;

@Component
public class ItemMapper {

    /**
     * Maps an item for an anonymous or non-owner viewer. {@code detailLocation} is always null here,
     * so callers that forget the viewer fail closed.
     */
    public ItemPostResponseDTO toResponse(ItemPost itemPost) {
        return toResponse(itemPost, null);
    }

    /**
     * Maps an item for a specific viewer. {@code detailLocation} is included only when the viewer
     * is the poster (docs/security.md: the finder's private detail is for the owner only).
     */
    public ItemPostResponseDTO toResponse(ItemPost itemPost, Long viewerId) {
        boolean viewerIsPoster = viewerId != null && viewerId.equals(itemPost.getUser().getId());
        String detailLocation = viewerIsPoster ? itemPost.getDetailLocation() : null;

        return new ItemPostResponseDTO(
            itemPost.getId(),
            itemPost.getUser().getId(),
            itemPost.getUser().getUsername(),
            itemPost.getTitle(),
            itemPost.getDescription(),
            itemPost.getCategory(),
            itemPost.getStatus(),
            itemPost.getImageUrl(),
            itemPost.getImageUrl2(),
            itemPost.getLocation() == null ? null : itemPost.getLocation().getY(),
            itemPost.getLocation() == null ? null : itemPost.getLocation().getX(),
            itemPost.getLocationText(),
            detailLocation,
            itemPost.getEventTime(),
            itemPost.getCreatedAt(),
            itemPost.getUpdatedAt(),
            itemPost.getPostType(),
            itemPost.getRewardCents(),
            itemPost.getResolvedWithUser() == null ? null : itemPost.getResolvedWithUser().getId(),
            itemPost.isPosterConfirmedResolution(),
            itemPost.isCounterpartyConfirmedResolution()
        );
    }
}
