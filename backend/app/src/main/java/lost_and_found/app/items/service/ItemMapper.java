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

        // A poster who turned on "Private Profile" has their name hidden from everyone else
        // (docs/design/ui-spec.md Settings; docs/security.md). The poster id stays so chat and
        // claims still route correctly; only the displayed name is withheld.
        boolean hideName = !viewerIsPoster && itemPost.getUser().isPrivateProfile();
        String posterUsername = hideName ? null : itemPost.getUser().getUsername();

        return new ItemPostResponseDTO(
            itemPost.getId(),
            itemPost.getUser().getId(),
            posterUsername,
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
