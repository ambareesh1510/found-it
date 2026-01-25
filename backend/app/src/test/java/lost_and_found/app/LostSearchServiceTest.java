package lost_and_found.app;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lost_and_found.app.exceptions.UserNotFoundException;
import lost_and_found.app.items.domain.ItemPost;
import lost_and_found.app.items.domain.ItemPostStatus;
import lost_and_found.app.items.domain.PostType;
import lost_and_found.app.items.service.PointUtil;
import lost_and_found.app.searches.domain.LostSearch;
import lost_and_found.app.searches.dto.request.LostSearchRequestDTO;
import lost_and_found.app.searches.dto.request.TimelinePointDTO;
import lost_and_found.app.searches.dto.response.LostSearchResultDTO;
import lost_and_found.app.searches.infra.LostSearchRepository;
import lost_and_found.app.searches.service.LostSearchService;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.test_implementations.TestItemPostRepository;
import lost_and_found.app.test_implementations.UserRepositoryTest;

class LostSearchServiceTest {

    private static final double LON = -84.3963;
    private static final double LAT = 33.7756;
    private static final Instant WINDOW_START = Instant.parse("2026-09-15T14:00:00Z");
    private static final Instant WINDOW_END = Instant.parse("2026-09-15T16:00:00Z");

    private final PointUtil pointUtil = new PointUtil();
    private UserRepositoryTest userRepository;
    private TestItemPostRepository itemRepository;
    private List<LostSearch> savedSearches;
    private LostSearchService service;
    private User user;

    @BeforeEach
    void setUp() {
        userRepository = new UserRepositoryTest();
        user = userRepository.save(User.builder()
            .username("saket")
            .email("saket@test.com")
            .password("encoded")
            .enabled(true)
            .build());

        itemRepository = new TestItemPostRepository();
        savedSearches = new ArrayList<>();

        // Only save() is exercised by the service, so the other repository methods are not implemented.
        LostSearchRepository searchRepository = (LostSearchRepository) Proxy.newProxyInstance(
            LostSearchRepository.class.getClassLoader(),
            new Class<?>[] { LostSearchRepository.class },
            (proxy, method, args) -> {
                if (method.getName().equals("save")) {
                    LostSearch search = (LostSearch) args[0];
                    search.setId(100L + savedSearches.size());
                    savedSearches.add(search);
                    return search;
                }
                throw new UnsupportedOperationException(method.getName());
            }
        );

        service = new LostSearchService(userRepository, searchRepository, itemRepository, pointUtil);
    }

    private ItemPost post(String category, ItemPostStatus status, PostType type, Instant eventTime, double lon, double lat) {
        ItemPost item = new ItemPost();
        item.setUser(user);
        item.setTitle("Black wallet");
        item.setCategory(category);
        item.setStatus(status);
        item.setPostType(type);
        item.setImageUrl("https://example.com/photo.jpg");
        item.setLocation(pointUtil.create(lon, lat));
        item.setEventTime(eventTime);
        return itemRepository.save(item);
    }

    private LostSearchRequestDTO request(String category) {
        return new LostSearchRequestDTO(category, LAT, LON, 1609.0, WINDOW_START, WINDOW_END, false, null);
    }

    @Test
    void returnsOpenFoundItemMatchingCategoryPlaceAndWindow_andRecordsDeclaration() {
        post("Wallet", ItemPostStatus.OPEN, PostType.FOUND, Instant.parse("2026-09-15T15:00:00Z"), LON, LAT);

        LostSearchResultDTO result = service.searchFoundItems(user.getId(), request("Wallet"));

        assertEquals(1, result.results().size());
        assertEquals("Black wallet", result.results().get(0).title());
        assertEquals(1, savedSearches.size(), "declaration must be persisted before results are returned");
        assertEquals("Wallet", savedSearches.get(0).getCategory());
        assertEquals(result.searchId(), savedSearches.get(0).getId());
        assertFalse(savedSearches.get(0).isUsedTimeline());
    }

    @Test
    void excludesLostPosts_resolvedPosts_otherCategories_andEventsOutsideWindow() {
        Instant inside = Instant.parse("2026-09-15T15:00:00Z");
        Instant outside = Instant.parse("2026-09-15T20:00:00Z");

        post("Wallet", ItemPostStatus.OPEN, PostType.LOST, inside, LON, LAT);
        post("Wallet", ItemPostStatus.RESOLVED, PostType.FOUND, inside, LON, LAT);
        post("Keys", ItemPostStatus.OPEN, PostType.FOUND, inside, LON, LAT);
        post("Wallet", ItemPostStatus.OPEN, PostType.FOUND, outside, LON, LAT);

        LostSearchResultDTO result = service.searchFoundItems(user.getId(), request("Wallet"));

        assertTrue(result.results().isEmpty());
    }

    @Test
    void categoryMatchIgnoresCase() {
        post("Wallet", ItemPostStatus.OPEN, PostType.FOUND, Instant.parse("2026-09-15T15:00:00Z"), LON, LAT);

        assertEquals(1, service.searchFoundItems(user.getId(), request("wallet")).results().size());
    }

    @Test
    void excludesFoundItemsOutsideRadius() {
        // About 1 degree of latitude is roughly 111 km, far outside a 1 mile search.
        post("Wallet", ItemPostStatus.OPEN, PostType.FOUND, Instant.parse("2026-09-15T15:00:00Z"), LON, LAT + 1.0);

        assertTrue(service.searchFoundItems(user.getId(), request("Wallet")).results().isEmpty());
    }

    @Test
    void unknownUserIsRejectedBeforeAnythingIsRecorded() {
        assertThrows(UserNotFoundException.class, () ->
            service.searchFoundItems(999L, request("Wallet"))
        );
        assertTrue(savedSearches.isEmpty());
    }

    @Test
    void timelineSearchRequiresPoints() {
        LostSearchRequestDTO timeline = new LostSearchRequestDTO("Wallet", null, null, null, null, null, true, List.of());

        assertFalse(timeline.isTimelineDeclarationValid());
        assertTrue(request("Wallet").isManualDeclarationValid());
    }

    @Test
    void timelineSearchUsesRecentPointsAndRecordsNoLocation() {
        Instant now = Instant.now();
        post("Wallet", ItemPostStatus.OPEN, PostType.FOUND, now.minusSeconds(60), LON, LAT);
        LostSearchRequestDTO timeline = new LostSearchRequestDTO(
            "Wallet", null, null, null, null, null, true,
            List.of(new TimelinePointDTO(LAT, LON, now.minusSeconds(120)))
        );

        LostSearchResultDTO result = service.searchFoundItems(user.getId(), timeline);

        assertEquals(1, result.results().size());
        assertTrue(savedSearches.get(0).isUsedTimeline());
        assertNull(savedSearches.get(0).getLocation());
        assertEquals(804.672, savedSearches.get(0).getSearchRadiusMeters());
    }

    @Test
    void windowMustEndAfterItStarts() {
        LostSearchRequestDTO backwards = new LostSearchRequestDTO("Wallet", LAT, LON, 1609.0, WINDOW_END, WINDOW_START, false, null);

        assertFalse(backwards.isManualDeclarationValid());
    }
}
