package lost_and_found.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import lost_and_found.app.auth.config.JwtAuthFilter;
import lost_and_found.app.auth.service.HS256JwtService;
import lost_and_found.app.auth.service.JwtService;
import lost_and_found.app.auth.service.WsTicketService;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.test_implementations.BlacklistServiceTest;
import lost_and_found.app.test_implementations.UserDetailsServiceTest;
import lost_and_found.app.test_implementations.UserRepositoryTest;

class JwtAuthFilterTest {

    private UserRepositoryTest users;
    private JwtService jwtService;
    private JwtAuthFilter filter;
    private User alice;

    @BeforeEach
    void setUp() {
        String secret = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
        users = new UserRepositoryTest();
        jwtService = new HS256JwtService(new BlacklistServiceTest(), secret, 60_000L);
        filter = new JwtAuthFilter((request, response, handler, ex) -> null, jwtService,
            new UserDetailsServiceTest(users));
        alice = users.save(User.builder().username("alice").email("alice@test.com").enabled(true).build());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bearerHeaderAuthenticatesTheRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/items/mine");
        request.addHeader("Authorization", "Bearer " + jwtService.generateToken(alice));

        assertEquals("alice", authenticatedUsernameFor(request));
    }

    @Test
    void cookieStillAuthenticatesTheRequestForTheWebApp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/items/mine");
        request.setCookies(new jakarta.servlet.http.Cookie("access_token", jwtService.generateToken(alice)));

        assertEquals("alice", authenticatedUsernameFor(request));
    }

    @Test
    void aWebSocketTicketCannotAuthenticateAnApiCall() throws Exception {
        String ticket = new WsTicketService(jwtService).issue(alice);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/items/mine");
        request.addHeader("Authorization", "Bearer " + ticket);

        assertNull(authenticatedUsernameFor(request));
    }

    @Test
    void aTokenForDeletedAccountDoesNotAuthenticateOrFail() throws Exception {
        String token = jwtService.generateToken(alice);
        users.deleteById(alice.getId());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/items/mine");
        request.addHeader("Authorization", "Bearer " + token);

        assertNull(authenticatedUsernameFor(request));
    }

    /** Runs the filter and returns the username authenticated while the rest of the chain runs, or null. */
    private String authenticatedUsernameFor(MockHttpServletRequest request) throws Exception {
        AtomicReference<String> username = new AtomicReference<>();
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) username.set(auth.getName());
        });
        return username.get();
    }
}
