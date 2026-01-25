package lost_and_found.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import lost_and_found.app.auth.service.HS256JwtService;
import lost_and_found.app.auth.service.JwtService;
import lost_and_found.app.auth.service.WsTicketService;
import lost_and_found.app.chat.infra.ChatController;
import lost_and_found.app.chat.service.ChatService;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.infra.GlobalExceptionHandler;
import lost_and_found.app.test_implementations.BlacklistServiceTest;
import lost_and_found.app.test_implementations.Fakes;

class ChatTicketControllerWebTest {

    private MockMvc mockMvc;
    private User alice;

    @BeforeEach
    void setUp() {
        String secret = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
        JwtService jwtService = new HS256JwtService(new BlacklistServiceTest(), secret, 60_000L);

        mockMvc = MockMvcBuilders
            .standaloneSetup(new ChatController(Fakes.unsupported(ChatService.class), new WsTicketService(jwtService)))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        alice = User.builder().id(7L).username("alice").email("alice@test.com").enabled(true).build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void wsTicket_issuesATicketForTheSignedInUser() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(alice, null, alice.getAuthorities()));

        mockMvc.perform(post("/chat/ws-ticket"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ticket").isString());
    }

    @Test
    void wsTicket_returnsUnauthorizedWithoutASession() throws Exception {
        mockMvc.perform(post("/chat/ws-ticket"))
            .andExpect(status().isUnauthorized());
    }
}
