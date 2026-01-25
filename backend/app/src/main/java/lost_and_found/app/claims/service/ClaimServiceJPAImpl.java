package lost_and_found.app.claims.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lost_and_found.app.claims.domain.ClaimStatus;
import lost_and_found.app.claims.domain.ItemClaim;
import lost_and_found.app.claims.dto.requests.CreateClaimDTO;
import lost_and_found.app.claims.dto.responses.ItemClaimResponseDTO;
import lost_and_found.app.claims.infra.ItemClaimRepository;
import lost_and_found.app.exceptions.ClaimNotExistException;
import lost_and_found.app.exceptions.ItemNotFoundException;
import lost_and_found.app.exceptions.UserMismatchException;
import lost_and_found.app.exceptions.UserNotFoundException;
import lost_and_found.app.items.domain.ItemPost;
import lost_and_found.app.items.domain.ItemPostStatus;
import lost_and_found.app.items.infra.ItemPostRepository;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.infra.UserRepository;

@Service
public class ClaimServiceJPAImpl implements ClaimService {

    private final ItemClaimRepository itemClaimRepository;
    private final ItemPostRepository itemPostRepository;
    private final UserRepository userRepository;
    private final ClaimMapper claimMapper;

    public ClaimServiceJPAImpl(
        ItemClaimRepository itemClaimRepository,
        ItemPostRepository itemPostRepository,
        UserRepository userRepository,
        ClaimMapper claimMapper
    ) {
        this.itemClaimRepository = itemClaimRepository;
        this.userRepository = userRepository;
        this.claimMapper = claimMapper;
        this.itemPostRepository = itemPostRepository;
    }


    @Transactional
    @Override
    public ItemClaimResponseDTO createClaim(Long claimantId, CreateClaimDTO req) 
        throws UserNotFoundException, ItemNotFoundException, IllegalStateException {
        User claimant = userRepository.findById(claimantId)
            .orElseThrow(() -> new UserNotFoundException("User " + claimantId + " not found"));

        ItemPost item = itemPostRepository.findById(req.itemId())
            .orElseThrow(() -> new ItemNotFoundException("Item " + req.itemId() + " not found"));

        if (Objects.equals(item.getUser().getId(), claimantId)) {
            throw new IllegalStateException("User " + claimantId + " cannot create a claim on their own item.");
        }

        if (item.getStatus() != ItemPostStatus.OPEN) {
            throw new IllegalStateException("Claims can only be created for OPEN items.");
        }

        if (itemClaimRepository.existsByItemPostIdAndClaimantUserId(item.getId(), claimantId)) {
            throw new IllegalStateException(
                "User " + claimantId + " already has a claim for item " + item.getId()
            );
        }

        ItemClaim claim = new ItemClaim();
        claim.setItemPost(item);
        claim.setClaimantUser(claimant);
        claim.setStatus(ClaimStatus.PENDING);

        ItemClaim saved = itemClaimRepository.save(claim);
        return claimMapper.toResponse(saved, claimantId);
    }


    @Transactional
    @Override
    public void deleteClaim(Long claimantId, Long claimId) 
        throws UserMismatchException, ClaimNotExistException, IllegalStateException {
        ItemClaim claim = itemClaimRepository.findById(claimId)
            .orElseThrow(() -> new ClaimNotExistException("Claim " + claimId + " does not exist"));

        if (!Objects.equals(claimantId, claim.getClaimantUser().getId())) {
            throw new UserMismatchException(
                "User " + claimantId + " tried to delete a claim that did not belong to them."
            );
        }

        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new IllegalStateException("Only PENDING claims can be deleted.");
        }

        itemClaimRepository.delete(claim);
    }

    @Transactional
    @Override
    public ItemClaimResponseDTO acceptClaim(Long ownerUserId, Long claimId)
            throws UserMismatchException, ClaimNotExistException, IllegalStateException {

        ItemClaim claim = itemClaimRepository.findById(claimId)
            .orElseThrow(() -> new ClaimNotExistException("Claim " + claimId + " does not exist"));

        ItemPost item = claim.getItemPost();

        if (Long.compare(ownerUserId, item.getUser().getId()) != 0) {
            throw new UserMismatchException("User " + ownerUserId + " does not own item " + item.getId() + " but tried to accept one of its claims.");
        }

        if (item.getStatus() != ItemPostStatus.OPEN) {
            throw new IllegalStateException("Only OPEN items can have claims accepted.");
        }

        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new IllegalStateException("Only PENDING claims can be accepted.");
        }

        claim.setStatus(ClaimStatus.ACCEPTED);
        item.setStatus(ItemPostStatus.RESOLVED);

        List<ItemClaim> itemClaims = itemClaimRepository.findByItemPostId(item.getId());
        for (ItemClaim otherClaim : itemClaims) {
            if (!Objects.equals(otherClaim.getId(), claim.getId())
                    && otherClaim.getStatus() == ClaimStatus.PENDING) {
                otherClaim.setStatus(ClaimStatus.REJECTED);
            }
        }

        ItemClaim saved = itemClaimRepository.save(claim);
        return claimMapper.toResponse(saved, ownerUserId);
    }

    @Transactional
    @Override
    public ItemClaimResponseDTO rejectClaim(Long ownerUserId, Long claimId)
            throws UserMismatchException, ClaimNotExistException, IllegalStateException {

        ItemClaim claim = itemClaimRepository.findById(claimId)
            .orElseThrow(() -> new ClaimNotExistException("Claim " + claimId + " does not exist"));

        ItemPost item = claim.getItemPost();

        if (Long.compare(ownerUserId, item.getUser().getId()) != 0) {
            throw new UserMismatchException("User " + ownerUserId + " does not own item " + item.getId() + " but tried to accept one of its claims.");
        }

        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new IllegalStateException("Only PENDING claims can be rejected.");
        }

        claim.setStatus(ClaimStatus.REJECTED);

        ItemClaim saved = itemClaimRepository.save(claim);
        return claimMapper.toResponse(saved, ownerUserId);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ItemClaimResponseDTO> getClaimsForItem(Long ownerUserId, Long itemId)
            throws UserMismatchException, ItemNotFoundException {

        ItemPost item = itemPostRepository.findById(itemId)
            .orElseThrow(() -> new ItemNotFoundException("Item " + itemId + " not found"));

        if (Long.compare(ownerUserId, item.getUser().getId()) != 0) {
            throw new UserMismatchException("User " + ownerUserId + " does not own item " + item.getId() + " but tried to accept one of its claims.");
        }

        return itemClaimRepository.findByItemPostId(itemId)
            .stream()
            .map(claim -> claimMapper.toResponse(claim, ownerUserId))
            .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<ItemClaimResponseDTO> getClaimsForUser(Long userId) throws UserNotFoundException {
        userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User " + userId + " not found"));

        return itemClaimRepository.findByClaimantUserId(userId)
            .stream()
            .map(claim -> claimMapper.toResponse(claim, userId))
            .toList();
    }

}
