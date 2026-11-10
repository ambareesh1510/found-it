package lost_and_found.app;

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

import lost_and_found.app.claims.domain.ClaimStatus;
import lost_and_found.app.claims.dto.responses.ItemClaimResponseDTO;
import lost_and_found.app.claims.infra.ClaimController;
import lost_and_found.app.claims.service.ClaimService;
import lost_and_found.app.items.domain.ItemPostStatus;
import lost_and_found.app.items.dto.response.ItemPostResponseDTO;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.infra.GlobalExceptionHandler;

class ClaimControllerWebTest {

    private MockMvc mockMvc;
    private FakeClaimService claimService;

    @BeforeEach
    void setUp() {
        claimService = new FakeClaimService();

        mockMvc = MockMvcBuilders
            .standaloneSetup(new ClaimController(claimService))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createClaim_shouldRejectMissingItemId() throws Exception {
        authenticateAs(buildUser(9L, "claimant"));

        mockMvc.perform(post("/claims")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("itemId: Item ID is required."));
    }

    @Test
    void createClaim_shouldUseAuthenticatedUserId() throws Exception {
        authenticateAs(buildUser(9L, "claimant"));

        claimService.responseToReturn = new ItemClaimResponseDTO(
            new ItemPostResponseDTO(
                5L,
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
                Instant.parse("2026-03-14T10:00:00Z")
            ),
            99L,
            9L,
            "claimant",
            ClaimStatus.PENDING,
            Instant.parse("2026-03-14T10:00:00Z"),
            Instant.parse("2026-03-14T10:00:00Z")
        );

        mockMvc.perform(post("/claims")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "itemId": 5
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.claimantId").value(9))
            .andExpect(jsonPath("$.claimId").value(99));

        org.junit.jupiter.api.Assertions.assertEquals(9L, claimService.lastClaimantId);
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

    private static final class FakeClaimService implements ClaimService {
        private Long lastClaimantId;
        private ItemClaimResponseDTO responseToReturn;

        @Override
        public ItemClaimResponseDTO createClaim(Long claimantId, lost_and_found.app.claims.dto.requests.CreateClaimDTO req) {
            lastClaimantId = claimantId;
            return responseToReturn;
        }

        @Override
        public void deleteClaim(Long claimantId, Long claimId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ItemClaimResponseDTO acceptClaim(Long ownerUserId, Long claimId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ItemClaimResponseDTO rejectClaim(Long ownerUserId, Long claimId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<ItemClaimResponseDTO> getClaimsForItem(Long ownerUserId, Long itemId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<ItemClaimResponseDTO> getClaimsForUser(Long userId) {
            throw new UnsupportedOperationException();
        }
    }
}
