package lost_and_found.app.claims.infra;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import lost_and_found.app.claims.domain.ClaimStatus;
import lost_and_found.app.claims.domain.ItemClaim;

public interface ItemClaimRepository extends JpaRepository<ItemClaim, Long> {

    List<ItemClaim> findByItemPostId(Long itemPostId);

    List<ItemClaim> findByClaimantUserId(Long claimantUserId);

    List<ItemClaim> findByStatus(ClaimStatus status);

    Optional<ItemClaim> findByItemPostIdAndClaimantUserId(Long itemPostId, Long claimantUserId);

    boolean existsByItemPostIdAndClaimantUserId(Long itemPostId, Long claimantUserId);
}
