package lost_and_found.app;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lost_and_found.app.claims.domain.ClaimStatus;
import lost_and_found.app.claims.domain.ItemClaim;
import lost_and_found.app.claims.dto.requests.CreateClaimDTO;
import lost_and_found.app.claims.dto.responses.ItemClaimResponseDTO;
import lost_and_found.app.claims.service.ClaimMapper;
import lost_and_found.app.claims.service.ClaimService;
import lost_and_found.app.claims.service.ClaimServiceJPAImpl;
import lost_and_found.app.exceptions.ClaimNotExistException;
import lost_and_found.app.exceptions.ItemNotFoundException;
import lost_and_found.app.exceptions.UserMismatchException;
import lost_and_found.app.exceptions.UserNotFoundException;
import lost_and_found.app.items.domain.ItemPost;
import lost_and_found.app.items.domain.ItemPostStatus;
import lost_and_found.app.items.service.ItemMapper;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.test_implementations.TestItemClaimRepository;
import lost_and_found.app.test_implementations.TestItemPostRepository;
import lost_and_found.app.test_implementations.UserRepositoryTest;

class ClaimServiceJPAImplTest {

    private ClaimService claimService;
    private TestItemClaimRepository itemClaimRepository;
    private TestItemPostRepository itemPostRepository;
    private UserRepositoryTest userRepository;
    private ClaimMapper claimMapper;
    private ItemMapper itemMapper;

    @BeforeEach
    void setUp() {
        itemClaimRepository = new TestItemClaimRepository();
        itemPostRepository = new TestItemPostRepository();
        userRepository = new UserRepositoryTest();
        itemMapper = new ItemMapper();
        claimMapper = new ClaimMapper(itemMapper);

        claimService = new ClaimServiceJPAImpl(
            itemClaimRepository,
            itemPostRepository,
            userRepository,
            claimMapper
        );
    }

    @Test
    void createClaim_shouldCreatePendingClaim() throws Exception {
        User owner = saveUser("owner", "owner@test.com");
        User claimant = saveUser("claimant", "claimant@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.OPEN);

        ItemClaimResponseDTO result = claimService.createClaim(
            claimant.getId(),
            new CreateClaimDTO(item.getId())
        );

        assertNotNull(result);
        assertNotNull(result.claimId());
        assertEquals(item.getId(), result.item().itemId());
        assertEquals(claimant.getId(), result.claimantId());
        assertEquals(ClaimStatus.PENDING, result.claimStatus());
    }

    @Test
    void createClaim_shouldThrowWhenClaimantMissing() {
        User owner = saveUser("owner", "owner@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.OPEN);

        assertThrows(UserNotFoundException.class, () ->
            claimService.createClaim(999L, new CreateClaimDTO(item.getId()))
        );
    }

    @Test
    void createClaim_shouldThrowWhenItemMissing() {
        User claimant = saveUser("claimant", "claimant@test.com");

        assertThrows(ItemNotFoundException.class, () ->
            claimService.createClaim(claimant.getId(), new CreateClaimDTO(999L))
        );
    }

    @Test
    void createClaim_shouldThrowWhenClaimingOwnItem() {
        User owner = saveUser("owner", "owner@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.OPEN);

        assertThrows(IllegalStateException.class, () ->
            claimService.createClaim(owner.getId(), new CreateClaimDTO(item.getId()))
        );
    }

    @Test
    void createClaim_shouldThrowWhenItemNotOpen() {
        User owner = saveUser("owner", "owner@test.com");
        User claimant = saveUser("claimant", "claimant@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.RESOLVED);

        assertThrows(IllegalStateException.class, () ->
            claimService.createClaim(claimant.getId(), new CreateClaimDTO(item.getId()))
        );
    }

    @Test
    void createClaim_shouldThrowWhenDuplicateClaimExists() throws Exception {
        User owner = saveUser("owner", "owner@test.com");
        User claimant = saveUser("claimant", "claimant@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.OPEN);

        claimService.createClaim(claimant.getId(), new CreateClaimDTO(item.getId()));

        assertThrows(IllegalStateException.class, () ->
            claimService.createClaim(claimant.getId(), new CreateClaimDTO(item.getId()))
        );
    }

    @Test
    void deleteClaim_shouldDeletePendingClaimOwnedByClaimant() throws Exception {
        User owner = saveUser("owner", "owner@test.com");
        User claimant = saveUser("claimant", "claimant@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.OPEN);

        ItemClaim claim = new ItemClaim();
        claim.setItemPost(item);
        claim.setClaimantUser(claimant);
        claim.setStatus(ClaimStatus.PENDING);
        claim = itemClaimRepository.save(claim);

        claimService.deleteClaim(claimant.getId(), claim.getId());

        assertTrue(itemClaimRepository.findById(claim.getId()).isEmpty());
    }

    @Test
    void deleteClaim_shouldThrowWhenClaimMissing() {
        assertThrows(ClaimNotExistException.class, () ->
            claimService.deleteClaim(1L, 999L)
        );
    }

    @Test
    void deleteClaim_shouldThrowWhenWrongUserDeletes() {
        User owner = saveUser("owner", "owner@test.com");
        User claimant = saveUser("claimant", "claimant@test.com");
        User other = saveUser("other", "other@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.OPEN);

        ItemClaim claim = new ItemClaim();
        claim.setItemPost(item);
        claim.setClaimantUser(claimant);
        claim.setStatus(ClaimStatus.PENDING);
        claim = itemClaimRepository.save(claim);

        Long claimId = claim.getId();

        assertThrows(UserMismatchException.class, () ->
            claimService.deleteClaim(other.getId(), claimId)
        );
    }

    @Test
    void deleteClaim_shouldThrowWhenClaimNotPending() {
        User owner = saveUser("owner", "owner@test.com");
        User claimant = saveUser("claimant", "claimant@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.OPEN);

        ItemClaim claim = new ItemClaim();
        claim.setItemPost(item);
        claim.setClaimantUser(claimant);
        claim.setStatus(ClaimStatus.ACCEPTED);
        claim = itemClaimRepository.save(claim);

        Long claimId = claim.getId();

        assertThrows(IllegalStateException.class, () ->
            claimService.deleteClaim(claimant.getId(), claimId)
        );
    }

    @Test
    void acceptClaim_shouldAcceptTargetRejectOthersAndResolveItem() throws Exception {
        User owner = saveUser("owner", "owner@test.com");
        User claimant1 = saveUser("claimant1", "claimant1@test.com");
        User claimant2 = saveUser("claimant2", "claimant2@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.OPEN);

        ItemClaim c1 = new ItemClaim();
        c1.setItemPost(item);
        c1.setClaimantUser(claimant1);
        c1.setStatus(ClaimStatus.PENDING);
        c1 = itemClaimRepository.save(c1);

        ItemClaim c2 = new ItemClaim();
        c2.setItemPost(item);
        c2.setClaimantUser(claimant2);
        c2.setStatus(ClaimStatus.PENDING);
        c2 = itemClaimRepository.save(c2);

        ItemClaimResponseDTO result = claimService.acceptClaim(owner.getId(), c1.getId());

        assertEquals(ClaimStatus.ACCEPTED, result.claimStatus());
        assertEquals(ClaimStatus.ACCEPTED, itemClaimRepository.findById(c1.getId()).orElseThrow().getStatus());
        assertEquals(ClaimStatus.REJECTED, itemClaimRepository.findById(c2.getId()).orElseThrow().getStatus());
        assertEquals(ItemPostStatus.RESOLVED, itemPostRepository.findById(item.getId()).orElseThrow().getStatus());
    }

    @Test
    void acceptClaim_shouldThrowWhenClaimMissing() {
        assertThrows(ClaimNotExistException.class, () ->
            claimService.acceptClaim(1L, 999L)
        );
    }

    @Test
    void acceptClaim_shouldThrowWhenWrongOwner() {
        User owner = saveUser("owner", "owner@test.com");
        User claimant = saveUser("claimant", "claimant@test.com");
        User other = saveUser("other", "other@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.OPEN);

        ItemClaim claim = new ItemClaim();
        claim.setItemPost(item);
        claim.setClaimantUser(claimant);
        claim.setStatus(ClaimStatus.PENDING);
        claim = itemClaimRepository.save(claim);

        Long claimId = claim.getId();

        assertThrows(UserMismatchException.class, () ->
            claimService.acceptClaim(other.getId(), claimId)
        );
    }

    @Test
    void acceptClaim_shouldThrowWhenItemNotOpen() {
        User owner = saveUser("owner", "owner@test.com");
        User claimant = saveUser("claimant", "claimant@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.RESOLVED);

        ItemClaim claim = new ItemClaim();
        claim.setItemPost(item);
        claim.setClaimantUser(claimant);
        claim.setStatus(ClaimStatus.PENDING);
        claim = itemClaimRepository.save(claim);

        Long claimId = claim.getId();

        assertThrows(IllegalStateException.class, () ->
            claimService.acceptClaim(owner.getId(), claimId)
        );
    }

    @Test
    void acceptClaim_shouldThrowWhenClaimNotPending() {
        User owner = saveUser("owner", "owner@test.com");
        User claimant = saveUser("claimant", "claimant@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.OPEN);

        ItemClaim claim = new ItemClaim();
        claim.setItemPost(item);
        claim.setClaimantUser(claimant);
        claim.setStatus(ClaimStatus.REJECTED);
        claim = itemClaimRepository.save(claim);

        Long claimId = claim.getId();

        assertThrows(IllegalStateException.class, () ->
            claimService.acceptClaim(owner.getId(), claimId)
        );
    }

    @Test
    void rejectClaim_shouldRejectPendingClaim() throws Exception {
        User owner = saveUser("owner", "owner@test.com");
        User claimant = saveUser("claimant", "claimant@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.OPEN);

        ItemClaim claim = new ItemClaim();
        claim.setItemPost(item);
        claim.setClaimantUser(claimant);
        claim.setStatus(ClaimStatus.PENDING);
        claim = itemClaimRepository.save(claim);

        ItemClaimResponseDTO result = claimService.rejectClaim(owner.getId(), claim.getId());

        assertEquals(ClaimStatus.REJECTED, result.claimStatus());
        assertEquals(ClaimStatus.REJECTED, itemClaimRepository.findById(claim.getId()).orElseThrow().getStatus());
    }

    @Test
    void rejectClaim_shouldThrowWhenWrongOwner() {
        User owner = saveUser("owner", "owner@test.com");
        User claimant = saveUser("claimant", "claimant@test.com");
        User other = saveUser("other", "other@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.OPEN);

        ItemClaim claim = new ItemClaim();
        claim.setItemPost(item);
        claim.setClaimantUser(claimant);
        claim.setStatus(ClaimStatus.PENDING);
        claim = itemClaimRepository.save(claim);

        Long claimId = claim.getId();

        assertThrows(UserMismatchException.class, () ->
            claimService.rejectClaim(other.getId(), claimId)
        );
    }

    @Test
    void rejectClaim_shouldThrowWhenClaimNotPending() {
        User owner = saveUser("owner", "owner@test.com");
        User claimant = saveUser("claimant", "claimant@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.OPEN);

        ItemClaim claim = new ItemClaim();
        claim.setItemPost(item);
        claim.setClaimantUser(claimant);
        claim.setStatus(ClaimStatus.ACCEPTED);
        claim = itemClaimRepository.save(claim);

        Long claimId = claim.getId();

        assertThrows(IllegalStateException.class, () ->
            claimService.rejectClaim(owner.getId(), claimId)
        );
    }

    @Test
    void getClaimsForItem_shouldReturnClaimsWhenOwnerMatches() throws Exception {
        User owner = saveUser("owner", "owner@test.com");
        User claimant1 = saveUser("claimant1", "claimant1@test.com");
        User claimant2 = saveUser("claimant2", "claimant2@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.OPEN);

        ItemClaim c1 = new ItemClaim();
        c1.setItemPost(item);
        c1.setClaimantUser(claimant1);
        c1.setStatus(ClaimStatus.PENDING);
        itemClaimRepository.save(c1);

        ItemClaim c2 = new ItemClaim();
        c2.setItemPost(item);
        c2.setClaimantUser(claimant2);
        c2.setStatus(ClaimStatus.REJECTED);
        itemClaimRepository.save(c2);

        List<ItemClaimResponseDTO> result = claimService.getClaimsForItem(owner.getId(), item.getId());

        assertEquals(2, result.size());
    }

    @Test
    void getClaimsForItem_shouldThrowWhenItemMissing() {
        assertThrows(ItemNotFoundException.class, () ->
            claimService.getClaimsForItem(1L, 999L)
        );
    }

    @Test
    void getClaimsForItem_shouldThrowWhenWrongOwner() {
        User owner = saveUser("owner", "owner@test.com");
        User other = saveUser("other", "other@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.OPEN);

        assertThrows(UserMismatchException.class, () ->
            claimService.getClaimsForItem(other.getId(), item.getId())
        );
    }

    @Test
    void getClaimsForUser_shouldReturnClaimsWhenUserExists() throws Exception {
        User owner = saveUser("owner", "owner@test.com");
        User claimant = saveUser("claimant", "claimant@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.OPEN);

        ItemClaim c1 = new ItemClaim();
        c1.setItemPost(item);
        c1.setClaimantUser(claimant);
        c1.setStatus(ClaimStatus.PENDING);
        itemClaimRepository.save(c1);

        ItemClaim c2 = new ItemClaim();
        c2.setItemPost(item);
        c2.setClaimantUser(claimant);
        c2.setStatus(ClaimStatus.REJECTED);
        itemClaimRepository.save(c2);

        List<ItemClaimResponseDTO> result = claimService.getClaimsForUser(claimant.getId());

        assertEquals(2, result.size());
    }

    @Test
    void getClaimsForUser_shouldThrowWhenUserMissing() {
        assertThrows(UserNotFoundException.class, () ->
            claimService.getClaimsForUser(999L)
        );
    }

    @Test
    void claimResponses_showDetailLocationOnlyToTheItemOwner() throws Exception {
        User owner = saveUser("owner", "owner@test.com");
        User claimant = saveUser("claimant", "claimant@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.OPEN);
        item.setDetailLocation("under the bench");
        itemPostRepository.save(item);

        ItemClaimResponseDTO created = claimService.createClaim(claimant.getId(), new CreateClaimDTO(item.getId()));
        assertNull(created.item().detailLocation());

        ItemClaimResponseDTO forClaimant = claimService.getClaimsForUser(claimant.getId()).get(0);
        assertNull(forClaimant.item().detailLocation());

        ItemClaimResponseDTO forOwner = claimService.getClaimsForItem(owner.getId(), item.getId()).get(0);
        assertEquals("under the bench", forOwner.item().detailLocation());
    }

    private User saveUser(String username, String email) {
        User user = User.builder()
            .username(username)
            .email(email)
            .password("pw")
            .enabled(true)
            .build();
        return userRepository.save(user);
    }

    private ItemPost saveItem(User owner, ItemPostStatus status) {
        ItemPost item = new ItemPost();
        item.setUser(owner);
        item.setTitle("title");
        item.setDescription("desc");
        item.setCategory("category");
        item.setStatus(status);
        item.setImageUrl("img");
        return itemPostRepository.save(item);
    }
}