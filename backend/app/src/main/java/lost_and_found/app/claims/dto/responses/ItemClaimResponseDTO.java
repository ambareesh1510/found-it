package lost_and_found.app.claims.dto.responses;

import java.time.Instant;

import lost_and_found.app.claims.domain.ClaimStatus;
import lost_and_found.app.items.dto.response.ItemPostResponseDTO;

public record ItemClaimResponseDTO(
    ItemPostResponseDTO item,
    Long claimId,
    Long claimantId,
    String claimantUsername,
    ClaimStatus claimStatus,
    Instant createdAt,
    Instant updatedAt
) {

}
