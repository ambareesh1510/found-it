package lost_and_found.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import lost_and_found.app.auth.service.BlacklistService;
import lost_and_found.app.auth.service.HS256JwtService;
import lost_and_found.app.auth.service.JwtService;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.infra.GlobalExceptionHandler;
import lost_and_found.app.shared.infra.UserController;
import lost_and_found.app.shared.service.AccountService;
import lost_and_found.app.test_implementations.BlacklistServiceTest;
import lost_and_found.app.test_implementations.FakeAppleTokens;
import lost_and_found.app.test_implementations.Fakes;
import lost_and_found.app.test_implementations.UserRepositoryTest;

class AccountControllerWebTest {

    private UserRepositoryTest users;
    private MockMvc mockMvc;
    private User alice;

    @BeforeEach
    void setUp() {
        String secret = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
        users = new UserRepositoryTest();
        BlacklistService blacklist = new BlacklistServiceTest();
        JwtService jwtService = new HS256JwtService(blacklist, secret, 60_000L);
        AccountService accountService = new AccountService(users,
            Fakes.recordingLostSearches(new ArrayList<>()), blacklist, jwtService, new FakeAppleTokens());

        mockMvc = MockMvcBuilders
            .standaloneSetup(new UserController(users, accountService))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        alice = users.save(User.builder().username("alice").email("alice@test.com").enabled(true).build());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deleteMe_returnsNoContentForTheSignedInUser() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(alice, null, alice.getAuthorities()));

        mockMvc.perform(delete("/users/me"))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteMe_returnsUnauthorizedWithoutASession() throws Exception {
        mockMvc.perform(delete("/users/me"))
            .andExpect(status().isUnauthorized());
    }
}
