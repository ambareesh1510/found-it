package lost_and_found.app;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lost_and_found.app.exceptions.ItemNotFoundException;
import lost_and_found.app.exceptions.UserMismatchException;
import lost_and_found.app.exceptions.UserNotFoundException;
import lost_and_found.app.items.domain.ItemPost;
import lost_and_found.app.items.domain.ItemPostStatus;
import lost_and_found.app.items.domain.PostType;
import lost_and_found.app.items.dto.request.CreateItemDTO;
import lost_and_found.app.items.dto.request.UpdateItemDTO;
import lost_and_found.app.items.dto.response.ItemPostResponseDTO;
import lost_and_found.app.items.service.ItemSearchCriteria;
import lost_and_found.app.items.service.ItemMapper;
import lost_and_found.app.items.service.ItemService;
import lost_and_found.app.items.service.ItemServiceJPAImpl;
import lost_and_found.app.items.service.PointUtil;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.test_implementations.TestItemPostRepository;
import lost_and_found.app.test_implementations.UserRepositoryTest;

class ItemServiceJPAImplTest {

    private ItemService itemService;
    private UserRepositoryTest userRepository;
    private TestItemPostRepository itemPostRepository;
    private PointUtil pointUtil;
    private ItemMapper itemMapper;

    @BeforeEach
    void setUp() {
        userRepository = new UserRepositoryTest();
        itemPostRepository = new TestItemPostRepository();
        pointUtil = new PointUtil();
        itemMapper = new ItemMapper();

        itemService = new ItemServiceJPAImpl(
            userRepository,
            itemPostRepository,
            pointUtil,
            itemMapper
        );
    }

    @Test
    void createItem_shouldCreateOpenItemForExistingUser() throws Exception {
        User owner = saveUser("owner", "owner@test.com");

        CreateItemDTO req = new CreateItemDTO(
            "Lost Backpack",
            "black backpack",
            "Bags",
            "image-url",
            null,
            -84.39,
            33.77,
            "Clough",
            "3rd floor",
            Instant.parse("2026-03-14T10:00:00Z")
        );

        ItemPostResponseDTO result = itemService.createItem(owner.getId(), req);

        assertNotNull(result);
        assertNotNull(result.posterId());
        assertEquals(owner.getId(), result.posterId());
        assertEquals("Lost Backpack", result.title());
        assertEquals(ItemPostStatus.OPEN, result.status());
        assertEquals("Clough", result.locationText());
        assertEquals("3rd floor", result.detailLocation());
    }

    @Test
    void createItem_shouldThrowWhenUserMissing() {
        CreateItemDTO req = new CreateItemDTO(
            "Lost Backpack",
            "black backpack",
            "Bags",
            "image-url",
            null,
            -84.39,
            33.77,
            "Clough",
            "3rd floor",
            Instant.parse("2026-03-14T10:00:00Z")
        );

        assertThrows(UserNotFoundException.class, () ->
            itemService.createItem(999L, req)
        );
    }

    @Test
    void updateItem_shouldUpdateFieldsWhenOwnerMatches() throws Exception {
        User owner = saveUser("owner", "owner@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.OPEN);

        UpdateItemDTO req = new UpdateItemDTO(
            item.getId(),
            "Updated Title",
            "Updated Desc",
            "Electronics",
            "new-image-url",
            null,
            33.78,
            -84.40,
            "Student Center",
            "4th floor",
            Instant.parse("2026-03-14T12:00:00Z"),
            null,
            null
        );

        ItemPostResponseDTO result = itemService.updateItem(owner.getId(), req);

        assertEquals("Updated Title", result.title());
        assertEquals("Updated Desc", result.description());
        assertEquals("Electronics", result.category());
        assertEquals("new-image-url", result.imageUrl());
        assertEquals("Student Center", result.locationText());
        assertEquals("4th floor", result.detailLocation());
        assertEquals(33.78, result.latitude());
        assertEquals(-84.40, result.longitude());
    }

    @Test
    void updateItem_shouldThrowWhenItemMissing() {
        User owner = saveUser("owner", "owner@test.com");

        UpdateItemDTO req = new UpdateItemDTO(
            999L,
            "Updated Title",
            "Updated Desc",
            "Electronics",
            "new-image-url",
            null,
            33.78,
            -84.40,
            "Student Center",
            "4th floor",
            Instant.parse("2026-03-14T12:00:00Z"),
            null,
            null
        );

        assertThrows(ItemNotFoundException.class, () ->
            itemService.updateItem(owner.getId(), req)
        );
    }

    @Test
    void updateItem_shouldThrowWhenWrongUserUpdates() {
        User owner = saveUser("owner", "owner@test.com");
        User other = saveUser("other", "other@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.OPEN);

        UpdateItemDTO req = new UpdateItemDTO(
            item.getId(),
            "Updated Title",
            "Updated Desc",
            "Electronics",
            "new-image-url",
            null,
            33.78,
            -84.40,
            "Student Center",
            "4th floor",
            Instant.parse("2026-03-14T12:00:00Z"),
            null,
            null
        );

        assertThrows(UserMismatchException.class, () ->
            itemService.updateItem(other.getId(), req)
        );
    }

    @Test
    void deleteItem_shouldDeleteWhenOwnerMatches() throws Exception {
        User owner = saveUser("owner", "owner@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.OPEN);

        Long itemId = item.getId();
        itemService.deleteItem(owner.getId(), itemId);

        assertTrue(itemPostRepository.findById(itemId).isEmpty());
    }

    @Test
    void deleteItem_shouldThrowWhenItemMissing() {
        User owner = saveUser("owner", "owner@test.com");

        assertThrows(ItemNotFoundException.class, () ->
            itemService.deleteItem(owner.getId(), 999L)
        );
    }

    @Test
    void deleteItem_shouldThrowWhenWrongUserDeletes() {
        User owner = saveUser("owner", "owner@test.com");
        User other = saveUser("other", "other@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.OPEN);

        assertThrows(UserMismatchException.class, () ->
            itemService.deleteItem(other.getId(), item.getId())
        );
    }

    @Test
    void getNearbyItems_shouldReturnNearbyMappedItems() {
        User owner = saveUser("owner", "owner@test.com");

        ItemPost a = saveItem(owner, ItemPostStatus.OPEN);
        a.setLocation(pointUtil.create(-84.3900, 33.7700));
        itemPostRepository.save(a);

        ItemPost b = saveItem(owner, ItemPostStatus.OPEN);
        b.setLocation(pointUtil.create(-84.3901, 33.7701));
        itemPostRepository.save(b);

        ItemPost far = saveItem(owner, ItemPostStatus.OPEN);
        far.setLocation(pointUtil.create(-84.5000, 33.9000));
        itemPostRepository.save(far);

        List<ItemPostResponseDTO> result = itemService.getNearbyItems(null, -84.3900, 33.7700, 500.0);

        assertEquals(2, result.size());
    }

    @Test
    void getItems_shouldReturnMappedItemsFromCriteria() {
        User owner = saveUser("owner", "owner@test.com");
        ItemPost a = saveItem(owner, ItemPostStatus.OPEN);
        ItemPost b = saveItem(owner, ItemPostStatus.RESOLVED);

        List<ItemPostResponseDTO> result = itemService.getItems(
            null, new ItemSearchCriteria(null, null, null)
        );

        assertEquals(2, result.size());
        assertEquals(a.getId(), result.get(0).itemId());
        assertEquals(b.getId(), result.get(1).itemId());
    }

    @Test
    void getItemById_shouldReturnMappedItemWhenFound() throws Exception {
        User owner = saveUser("owner", "owner@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.OPEN);

        ItemPostResponseDTO result = itemService.getItemById(null, item.getId());
        assertEquals(item.getId(), result.itemId());
        assertEquals(owner.getId(), result.posterId());
        assertEquals(ItemPostStatus.OPEN, result.status());
    }

    @Test
    void getItemById_shouldThrowWhenMissing() {
        assertThrows(ItemNotFoundException.class, () ->
            itemService.getItemById(null, 999L)
        );
    }

    @Test
    void getItemById_showsDetailLocationOnlyToThePoster() throws Exception {
        User owner = saveUser("owner", "owner@test.com");
        User stranger = saveUser("stranger", "stranger@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.OPEN);
        item.setDetailLocation("under the bench");
        itemPostRepository.save(item);

        assertEquals("under the bench", itemService.getItemById(owner.getId(), item.getId()).detailLocation());
        assertNull(itemService.getItemById(stranger.getId(), item.getId()).detailLocation());
        assertNull(itemService.getItemById(null, item.getId()).detailLocation());
    }

    @Test
    void getItemsAndNearby_hideDetailLocationFromNonPosters() {
        User owner = saveUser("owner", "owner@test.com");
        ItemPost item = saveItem(owner, ItemPostStatus.OPEN);
        item.setDetailLocation("under the bench");
        itemPostRepository.save(item);

        assertNull(itemService.getItems(null, new ItemSearchCriteria(null, null, null)).get(0).detailLocation());
        assertNull(itemService.getNearbyItems(null, -84.39, 33.77, 500.0).get(0).detailLocation());
        assertEquals("under the bench",
            itemService.getNearbyItems(owner.getId(), -84.39, 33.77, 500.0).get(0).detailLocation());
    }

    @Test
    void createItem_allowsNoPhotoAndStoresLostReward() throws Exception {
        User owner = saveUser("owner", "owner@test.com");
        CreateItemDTO req = new CreateItemDTO(
            "Keys", "ring of three", "Keys", "  ", null, 33.77, -84.39,
            "Clough", null, Instant.parse("2026-03-14T10:00:00Z"), PostType.LOST, 5000
        );

        ItemPostResponseDTO result = itemService.createItem(owner.getId(), req);

        assertNull(result.imageUrl());
        assertEquals(5000, result.rewardCents());
        assertEquals(PostType.LOST, result.postType());
    }

    @Test
    void createItem_dropsRewardForFoundPosts() throws Exception {
        User finder = saveUser("finder", "finder@test.com");
        CreateItemDTO req = new CreateItemDTO(
            "Keys", null, "Keys", "https://example.com/keys.jpg", null, 33.77, -84.39,
            "Clough", null, Instant.parse("2026-03-14T10:00:00Z"), PostType.FOUND, 5000
        );

        ItemPostResponseDTO result = itemService.createItem(finder.getId(), req);

        assertEquals(PostType.FOUND, result.postType());
        assertNull(result.rewardCents());
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
        item.setLocation(pointUtil.create(-84.39, 33.77));
        item.setLocationText("location-text");
        return itemPostRepository.save(item);
    }
}
