package lost_and_found.app.claims.dto.requests;

import jakarta.validation.constraints.NotNull;

public record CreateClaimDTO(
    @NotNull(message = "Item ID is required.")
    Long itemId
) {

}
