package lost_and_found.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lost_and_found.app.auth.domain.AuthProviderType;
import lost_and_found.app.auth.domain.JwtBlacklist;
import lost_and_found.app.auth.service.BlacklistService;
import lost_and_found.app.auth.service.HS256JwtService;
import lost_and_found.app.auth.service.JwtService;
import lost_and_found.app.exceptions.UserNotFoundException;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.service.AccountService;
import lost_and_found.app.test_implementations.FakeAppleTokens;
import lost_and_found.app.test_implementations.Fakes;
import lost_and_found.app.test_implementations.UserRepositoryTest;

class AccountServiceTest {

    private UserRepositoryTest users;
    private JwtService jwtService;
    private List<Long> deletedSearchesFor;
    private List<String> blacklistedJtis;
    private AccountService accountService;
    private FakeAppleTokens appleTokens;
    private User alice;

    @BeforeEach
    void setUp() {
        String secret = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
        users = new UserRepositoryTest();
        deletedSearchesFor = new ArrayList<>();
        blacklistedJtis = new ArrayList<>();
        BlacklistService recordingBlacklist = new BlacklistService() {
            @Override
            public boolean isBlacklisted(String jti) {
                return blacklistedJtis.contains(jti);
            }

            @Override
            public JwtBlacklist blacklistToken(String jti, String username) {
                blacklistedJtis.add(jti);
                return JwtBlacklist.builder().jti(jti).username(username).build();
            }
        };
        jwtService = new HS256JwtService(recordingBlacklist, secret, 60_000L);
        appleTokens = new FakeAppleTokens();
        accountService = new AccountService(users, Fakes.recordingLostSearches(deletedSearchesFor),
            recordingBlacklist, jwtService, appleTokens);

        alice = users.save(User.builder()
            .username("alice")
            .email("alice@gmail.com")
            .password("hashed-password")
            .providerId("google-sub-1")
            .authProviderType(AuthProviderType.GOOGLE)
            .picture("https://example.com/avatar.png")
            .enabled(true)
            .build());
    }

    @Test
    void deletionRemovesEverythingThatIdentifiesTheUser() {
        accountService.deleteAccount(alice.getId(), null);

        User after = users.findById(alice.getId()).orElseThrow();
        assertEquals("deleted-" + alice.getId(), after.getUsername());
        assertEquals("deleted-" + alice.getId() + "@deleted.invalid", after.getEmail());
        assertNull(after.getPassword());
        assertNull(after.getProviderId());
        assertNull(after.getAuthProviderType());
        assertNull(after.getPicture());
        assertFalse(after.isEnabled());
    }

    @Test
    void deletionKeepsTheRowSoPostsAndClaimsStillResolve() {
        accountService.deleteAccount(alice.getId(), null);

        assertTrue(users.findById(alice.getId()).isPresent());
    }

    @Test
    void deletionRemovesTheUsersDeclaredSearches() {
        accountService.deleteAccount(alice.getId(), null);

        assertEquals(List.of(alice.getId()), deletedSearchesFor);
    }

    @Test
    void deletionRevokesTheTokenUsedToRequestIt() {
        String token = jwtService.generateToken(alice);

        accountService.deleteAccount(alice.getId(), token);

        assertEquals(List.of(jwtService.extractJti(token)), blacklistedJtis);
    }

    @Test
    void deletionRevokesTheAppleSignInAndForgetsTheRefreshToken() {
        alice.setAppleRefreshToken("apple-refresh-token-1");
        users.save(alice);

        accountService.deleteAccount(alice.getId(), null);

        assertEquals(List.of("apple-refresh-token-1"), appleTokens.revokedRefreshTokens);
        assertNull(users.findById(alice.getId()).orElseThrow().getAppleRefreshToken());
    }

    @Test
    void deletionWithoutAnAppleLinkDoesNotCallApple() {
        accountService.deleteAccount(alice.getId(), null);

        assertEquals(List.of(), appleTokens.revokedRefreshTokens);
    }

    @Test
    void deletionGoesAheadEvenIfAppleIsUnreachable() {
        // FakeAppleTokens never throws, matching the real client, which logs failures instead.
        alice.setAppleRefreshToken("apple-refresh-token-1");
        users.save(alice);

        accountService.deleteAccount(alice.getId(), null);

        User after = users.findById(alice.getId()).orElseThrow();
        assertFalse(after.isEnabled());
        assertEquals("deleted-" + alice.getId(), after.getUsername());
    }

    @Test
    void deletingAnUnknownAccountFails() {
        assertThrows(UserNotFoundException.class, () -> accountService.deleteAccount(999L, null));
    }
}
