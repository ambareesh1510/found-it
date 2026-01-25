package lost_and_found.app;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import lost_and_found.app.items.domain.ItemPostStatus;
import lost_and_found.app.items.dto.response.ItemPostResponseDTO;
import lost_and_found.app.items.infra.ItemController;
import lost_and_found.app.items.service.ItemSearchCriteria;
import lost_and_found.app.items.service.ItemSearchCriteriaMapper;
import lost_and_found.app.items.service.ItemService;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.infra.GlobalExceptionHandler;

class ItemControllerWebTest {

    private MockMvc mockMvc;
    private FakeItemService itemService;
    private FakeCriteriaMapper criteriaMapper;

    @BeforeEach
    void setUp() {
        itemService = new FakeItemService();
        criteriaMapper = new FakeCriteriaMapper();

        mockMvc = MockMvcBuilders
            .standaloneSetup(new ItemController(itemService, criteriaMapper))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createItem_shouldRejectMissingLatitude() throws Exception {
        authenticateAs(buildUser(1L, "poster"));

        mockMvc.perform(post("/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Lost Backpack",
                      "description": "black backpack",
                      "category": "Bags",
                      "imageUrl": "https://example.com/image.jpg",
                      "longitude": -84.39,
                      "locationText": "Clough",
                      "eventTime": "2026-03-14T10:00:00Z"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("latitude")));
    }

    @Test
    void createItem_shouldAllowSmallClockSkewForEventTime() throws Exception {
        authenticateAs(buildUser(1L, "poster"));
        Instant now = Instant.now();
        itemService.itemToReturn = new ItemPostResponseDTO(
            7L,
            1L,
            "poster",
            "Lost Backpack",
            "black backpack",
            "Bags",
            ItemPostStatus.OPEN,
            "image-url",
            null,
            33.77,
            -84.39,
            "Clough",
            "3rd floor",
            now,
            now,
            now,
            List.of()
        );

        mockMvc.perform(post("/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Lost Backpack",
                      "description": "black backpack",
                      "category": "Bags",
                      "imageUrl": "https://example.com/image.jpg",
                      "latitude": 33.77,
                      "longitude": -84.39,
                      "locationText": "Clough",
                      "eventTime": "%s"
                    }
                    """.formatted(Instant.now().plusSeconds(1))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.itemId").value(7));
    }

    @Test
    void updateItem_shouldRejectHalfLocationUpdate() throws Exception {
        authenticateAs(buildUser(1L, "poster"));

        mockMvc.perform(patch("/items/7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "itemId": 7,
                      "latitude": 33.77
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("Latitude and longitude must both be provided")));
    }

    @Test
    void getItems_shouldUseCriteriaMapper() throws Exception {
        ItemSearchCriteria criteria = new ItemSearchCriteria(null, null, null);
        criteriaMapper.criteriaToReturn = criteria;
        itemService.itemsToReturn = List.of(
            new ItemPostResponseDTO(
                7L,
                1L,
                "poster",
                "Lost Backpack",
                "black backpack",
                "Bags",
                ItemPostStatus.OPEN,
                "image-url",
                null,
                33.77,
                -84.39,
                "Clough",
                "3rd floor",
                Instant.parse("2026-03-14T10:00:00Z"),
                Instant.parse("2026-03-14T10:00:00Z"),
                Instant.parse("2026-03-14T10:00:00Z"),
                List.of()
            )
        );

        mockMvc.perform(get("/items"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].itemId").value(7));

        org.junit.jupiter.api.Assertions.assertNull(criteriaMapper.lastStatus);
        org.junit.jupiter.api.Assertions.assertNull(criteriaMapper.lastCategory);
        org.junit.jupiter.api.Assertions.assertNull(criteriaMapper.lastUserId);
        org.junit.jupiter.api.Assertions.assertSame(criteria, itemService.lastCriteria);
    }

    @Test
    void getItems_shouldForwardFilterParameters() throws Exception {
        ItemSearchCriteria criteria = new ItemSearchCriteria(ItemPostStatus.OPEN, null, null);
        criteriaMapper.criteriaToReturn = criteria;

        mockMvc.perform(get("/items")
                .param("status", "open"))
            .andExpect(status().isOk());

        org.junit.jupiter.api.Assertions.assertEquals("open", criteriaMapper.lastStatus);
        org.junit.jupiter.api.Assertions.assertNull(criteriaMapper.lastCategory);
        org.junit.jupiter.api.Assertions.assertNull(criteriaMapper.lastUserId);
        org.junit.jupiter.api.Assertions.assertSame(criteria, itemService.lastCriteria);
    }

    @Test
    void getItems_shouldReturnBadRequestForInvalidFilterInput() throws Exception {
        criteriaMapper.exceptionToThrow = new IllegalArgumentException("status must be one of: OPEN, RESOLVED.");

        mockMvc.perform(get("/items").param("status", "unsupported"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("status must be one of: OPEN, RESOLVED."));
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    private User buildUser(Long id, String username) {
        return User.builder()
            .id(id)
            .username(username)
            .email(username + "@test.com")
            .password("pass1234567")
            .enabled(true)
            .build();
    }

    private static final class FakeCriteriaMapper extends ItemSearchCriteriaMapper {
        private String lastStatus;
        private String lastCategory;
        private Long lastUserId;
        private ItemSearchCriteria criteriaToReturn;
        private RuntimeException exceptionToThrow;

        @Override
        public ItemSearchCriteria fromQueryParams(
            String status,
            String category,
            Long userId,
            String postType
        ) {
            lastStatus = status;
            lastCategory = category;
            lastUserId = userId;
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return criteriaToReturn;
        }
    }

    private static final class FakeItemService implements ItemService {
        private ItemSearchCriteria lastCriteria;
        private List<ItemPostResponseDTO> itemsToReturn = List.of();
        private ItemPostResponseDTO itemToReturn;

        @Override
        public ItemPostResponseDTO createItem(Long userId, lost_and_found.app.items.dto.request.CreateItemDTO req) {
            return itemToReturn;
        }

        @Override
        public ItemPostResponseDTO updateItem(Long userId, lost_and_found.app.items.dto.request.UpdateItemDTO req) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteItem(Long userId, Long itemId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ItemPostResponseDTO> getNearbyItems(Long viewerId, double lon, double lat, double radiusMeters) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ItemPostResponseDTO> getItems(Long viewerId, ItemSearchCriteria criteria) {
            lastCriteria = criteria;
            return itemsToReturn;
        }

        @Override
        public ItemPostResponseDTO getItemById(Long viewerId, Long itemId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ItemPostResponseDTO confirmResolution(Long userId, Long itemId) {
            throw new UnsupportedOperationException();
        }
    }
}
