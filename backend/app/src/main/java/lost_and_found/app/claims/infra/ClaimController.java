package lost_and_found.app.claims.infra;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import lost_and_found.app.claims.dto.requests.CreateClaimDTO;
import lost_and_found.app.claims.dto.responses.ItemClaimResponseDTO;
import lost_and_found.app.claims.service.ClaimService;
import lost_and_found.app.exceptions.UserNotFoundException;
import lost_and_found.app.shared.domain.User;


@RestController
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @PostMapping("/claims")
    public ResponseEntity<ItemClaimResponseDTO> createClaim(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody CreateClaimDTO req
    ) {
        if (user == null) throw new UserNotFoundException("User Not Authenticated");
        return ResponseEntity.ok(claimService.createClaim(user.getId(), req));
    }

    @DeleteMapping("/claims/{claimId}")
    public ResponseEntity<Void> deleteClaim(
        @AuthenticationPrincipal User user,
        @PathVariable Long claimId
    ) {
        if (user == null) throw new UserNotFoundException("User Not Authenticated");
        claimService.deleteClaim(user.getId(), claimId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/claims/{claimId}/accept")
    public ResponseEntity<ItemClaimResponseDTO> acceptClaim(
        @AuthenticationPrincipal User user,
        @PathVariable Long claimId
    ) {
        if (user == null) throw new UserNotFoundException("User Not Authenticated");
        return ResponseEntity.ok(claimService.acceptClaim(user.getId(), claimId));
    }

    @PatchMapping("/claims/{claimId}/reject")
    public ResponseEntity<ItemClaimResponseDTO> rejectClaim(
        @AuthenticationPrincipal User user,
        @PathVariable Long claimId
    ) {
        if (user == null) throw new UserNotFoundException("User Not Authenticated");
        return ResponseEntity.ok(claimService.rejectClaim(user.getId(), claimId));
    }

    @GetMapping("/items/{itemId}/claims")
    public ResponseEntity<List<ItemClaimResponseDTO>> getClaimsForItem(
        @AuthenticationPrincipal User user,
        @PathVariable Long itemId
    ) {
        if (user == null) throw new UserNotFoundException("User Not Authenticated");
        return ResponseEntity.ok(claimService.getClaimsForItem(user.getId(), itemId));
    }

    @GetMapping("/claims/me")
    public ResponseEntity<List<ItemClaimResponseDTO>> getClaimsForUser(
        @AuthenticationPrincipal User user
    ) {
        if (user == null) throw new UserNotFoundException("User Not Authenticated");
        return ResponseEntity.ok(claimService.getClaimsForUser(user.getId()));
    }
}
