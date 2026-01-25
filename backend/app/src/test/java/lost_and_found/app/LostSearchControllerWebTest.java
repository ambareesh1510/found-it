package lost_and_found.app;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import lost_and_found.app.searches.dto.request.LostSearchRequestDTO;
import lost_and_found.app.searches.dto.response.LostSearchResultDTO;
import lost_and_found.app.searches.infra.LostSearchController;
import lost_and_found.app.searches.service.LostSearchService;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.infra.GlobalExceptionHandler;

class LostSearchControllerWebTest {

    private MockMvc mockMvc;
    private FakeLostSearchService service;

    @BeforeEach
    void setUp() {
        service = new FakeLostSearchService();
        mockMvc = MockMvcBuilders.standaloneSetup(new LostSearchController(service))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void timelineSearchAcceptsPointsAndUsesAuthenticatedUser() throws Exception {
        User user = User.builder().id(42L).username("loser").email("loser@test.com").enabled(true).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );

        mockMvc.perform(post("/searches/lost")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "category": "Keys",
                      "useTimeline": true,
                      "timelinePoints": [{
                        "latitude": 33.775,
                        "longitude": -84.396,
                        "recordedAt": "2026-10-21T12:00:00Z"
                      }]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.searchId").value(77));

        assertTrue(service.request.useTimeline());
        org.junit.jupiter.api.Assertions.assertEquals(42L, service.userId);
    }

    @Test
    void timelineSearchRejectsMissingPoints() throws Exception {
        User user = User.builder().id(42L).username("loser").email("loser@test.com").enabled(true).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );

        mockMvc.perform(post("/searches/lost")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"category": "Keys", "useTimeline": true, "timelinePoints": []}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("timelineDeclarationValid: Timeline searches require at least one location point."));
    }

    private static final class FakeLostSearchService extends LostSearchService {
        private Long userId;
        private LostSearchRequestDTO request;

        private FakeLostSearchService() {
            super(null, null, null, null);
        }

        @Override
        public LostSearchResultDTO searchFoundItems(Long userId, LostSearchRequestDTO request) {
            this.userId = userId;
            this.request = request;
            return new LostSearchResultDTO(77L, List.of());
        }
    }
}
