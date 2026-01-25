package lost_and_found.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import lost_and_found.app.auth.service.HS256JwtService;
import lost_and_found.app.auth.service.JwtService;
import lost_and_found.app.auth.service.WsTicketService;
import lost_and_found.app.chat.realtime.ChatWebSocketAuthHandshakeInterceptor;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.test_implementations.BlacklistServiceTest;
import lost_and_found.app.test_implementations.UserDetailsServiceTest;
import lost_and_found.app.test_implementations.UserRepositoryTest;

class ChatWebSocketTicketTest {

    private UserRepositoryTest users;
    private JwtService jwtService;
    private WsTicketService tickets;
    private ChatWebSocketAuthHandshakeInterceptor interceptor;
    private User alice;

    @BeforeEach
    void setUp() {
        String secret = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
        users = new UserRepositoryTest();
        jwtService = new HS256JwtService(new BlacklistServiceTest(), secret, 60_000L);
        tickets = new WsTicketService(jwtService);
        interceptor = new ChatWebSocketAuthHandshakeInterceptor(jwtService, new UserDetailsServiceTest(users));
        alice = users.save(User.builder().username("alice").email("alice@test.com").enabled(true).build());
    }

    @Test
    void aValidTicketOpensTheSocketAsItsUser() {
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = handshake("ticket=" + tickets.issue(alice), attributes);

        assertTrue(accepted);
        assertEquals(alice.getId(), attributes.get("userId"));
        assertEquals("alice", attributes.get("username"));
    }

    @Test
    void aSessionTokenInTheUrlIsRejected() {
        String sessionToken = jwtService.generateToken(alice);

        assertFalse(handshake("ticket=" + sessionToken, new HashMap<>()));
    }

    @Test
    void aMissingTicketIsRejected() {
        assertFalse(handshake(null, new HashMap<>()));
    }

    @Test
    void agarbageTicketIsRejectedWithoutAnError() {
        assertFalse(handshake("ticket=not-a-jwt", new HashMap<>()));
    }

    @Test
    void anExpiredTicketIsRejected() {
        String expired = jwtService.generateShortLivedToken(
            Map.of(WsTicketService.TYPE_CLAIM, WsTicketService.TICKET_TYPE), alice, -60_000L);

        assertFalse(handshake("ticket=" + expired, new HashMap<>()));
    }

    private boolean handshake(String query, Map<String, Object> attributes) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws/chat");
        servletRequest.setServerName("localhost");
        servletRequest.setQueryString(query);
        return interceptor.beforeHandshake(
            new ServletServerHttpRequest(servletRequest), null, null, attributes);
    }
}
