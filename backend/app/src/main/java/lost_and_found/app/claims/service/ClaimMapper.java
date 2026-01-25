package lost_and_found.app.claims.service;

import org.springframework.stereotype.Component;

import lost_and_found.app.claims.domain.ItemClaim;
import lost_and_found.app.claims.dto.responses.ItemClaimResponseDTO;
import lost_and_found.app.items.service.ItemMapper;

/**
 * a class that maps claim db entities to response DTOs
 */
@Component
public class ClaimMapper {
    private final ItemMapper itemMapper;

    public ClaimMapper(ItemMapper itemMapper) {
        this.itemMapper = itemMapper;
    }

    /**
     * a function that maps claim db entities to response DTOs
     * @param claim the claim db entity.
     * @param viewerId the requesting user; controls whether the item's detailLocation is included.
     * @return a claim dto response.
     */
    public ItemClaimResponseDTO toResponse(ItemClaim claim, Long viewerId) {
        return new ItemClaimResponseDTO(
            itemMapper.toResponse(claim.getItemPost(), viewerId),
            claim.getId(),
            claim.getClaimantUser().getId(),
            claim.getClaimantUser().getUsername(),
            claim.getStatus(), 
            claim.getCreatedAt(), 
            claim.getUpdatedAt()
        );
    }
}
