package lost_and_found.app.claims.service;

import java.util.List;

import lost_and_found.app.claims.dto.requests.CreateClaimDTO;
import lost_and_found.app.claims.dto.responses.ItemClaimResponseDTO;
import lost_and_found.app.exceptions.ClaimNotExistException;
import lost_and_found.app.exceptions.ItemNotFoundException;
import lost_and_found.app.exceptions.UserMismatchException;
import lost_and_found.app.exceptions.UserNotFoundException;

/**
 * defines the business logic for claims.
 */
public interface ClaimService {
    /**
     * A function that creates a claim.
     * @param claimantId the claim creator's id.
     * @param req the request containing claim info.
     * @return An item claim DTO.
     * @throws UserNotFoundException if the claimantId is not associated with a user.
     * @throws ItemNotFoundException if the linked item is not found.
     * @throws IllegalStateException if the creation is illegal in business logic (reason in message).
     */
    ItemClaimResponseDTO createClaim(Long claimantId, CreateClaimDTO req) 
        throws UserNotFoundException, ItemNotFoundException, IllegalStateException;
    /**
     * A function that deletes a claim.
     * @param claimantId the claim creator's id.
     * @param claimId the id of the claim to be deleted.
     * @throws UserMismatchException if the claimantId does not match the id belonging to the claim.
     * @throws ClaimNotExistException if the claim to be updated does not exist.
     * @throws IllegalStateException if the deletion is illegal in business logic (reason in message).
     */
    void deleteClaim(Long claimantId, Long claimId)
        throws UserMismatchException, ClaimNotExistException, IllegalStateException;
    /**
     * A function that accepts a claim for an item and does all related state changes.
     * @param ownerUserId the userid of the owner of the item.
     * @param claimId the claim to be accepted
     * @return the accepted claim.
     * @throws UserMismatchException if the ownerUserId does not match the id belonging to the item.
     * @throws ClaimNotExistException if the claim doesn't exist.
     * @throws IllegalStateException if the accept is illegal in business logic (reason in message).
     */
    ItemClaimResponseDTO acceptClaim(Long ownerUserId, Long claimId)
        throws UserMismatchException, ClaimNotExistException, IllegalStateException;
    /**
     * A function that rejects a claim for an item and does all related state changes.
     * @param ownerUserId the userid of the owner of the item.
     * @param claimId the claim to be rejected
     * @return the rejected claim.
     * @throws UserMismatchException if the ownerUserId does not match the id belonging to the item.
     * @throws ClaimNotExistException if the claim doesn't exist.
     * @throws IllegalStateException if the reject is illegal in business logic (reason in message).
     */
    ItemClaimResponseDTO rejectClaim(Long ownerUserId, Long claimId)
        throws UserMismatchException, ClaimNotExistException, IllegalStateException;
    /**
     * Gets all claims for an item.
     * @param ownerUserId the owner of the item.
     * @param itemId the item itself.
     * @return a list of all claims belonging to the item.
     * @throws UserNotFoundException the user is non-existent.
     * @throws ItemNotFoundException the item is non-existent. 
     */
    List<ItemClaimResponseDTO> getClaimsForItem(Long ownerUserId, Long itemId)
        throws UserMismatchException, ItemNotFoundException;
    /**
     * Gets all claims for a user.
     * @param ownerUserId the owner of the item.
     * @return a list of all claims belonging to the user.
     * @throws UserNotFoundException the user is non-existent.
     */ 
    List<ItemClaimResponseDTO> getClaimsForUser(Long userId)
        throws UserNotFoundException;

}
