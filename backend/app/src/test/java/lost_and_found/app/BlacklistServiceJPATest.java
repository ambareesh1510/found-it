package lost_and_found.app;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lost_and_found.app.auth.domain.JwtBlacklist;
import lost_and_found.app.auth.infra.JwtBlacklistRepository;
import lost_and_found.app.auth.service.BlacklistServiceJPA;
import lost_and_found.app.test_implementations.JwtBlacklistRepositoryTest;

class BlacklistServiceJPATest {

    private JwtBlacklistRepository jwtBlacklistRepository;
    private BlacklistServiceJPA blacklistService;

    @BeforeEach
    void setUp() {
        jwtBlacklistRepository = new JwtBlacklistRepositoryTest();
        blacklistService = new BlacklistServiceJPA(jwtBlacklistRepository);
    }

    @Test
    void isBlacklisted_shouldReturnFalse_whenJtiNotPresent() {
        assertFalse(blacklistService.isBlacklisted("missing-jti"));
    }

    @Test
    void blacklistToken_shouldSaveToken_andReturnSavedEntity() {
        String jti = "fff44833-b10b-4378-8ea6-9e15d4d83a4c";
        String username = "saket";

        JwtBlacklist saved = blacklistService.blacklistToken(jti, username);

        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals(jti, saved.getJti());
        assertEquals(username, saved.getUsername());
        assertNotNull(saved.getBlacklistedAt());

        // sanity check timestamp is "recent"
        assertFalse(saved.getBlacklistedAt().isAfter(Instant.now().plusSeconds(1)));

        assertTrue(blacklistService.isBlacklisted(jti));
        assertEquals(1, jwtBlacklistRepository.count());
    }

    @Test
    void isBlacklisted_shouldReturnTrue_afterBlacklistToken() {
        String jti = "test-jti-123";

        assertFalse(blacklistService.isBlacklisted(jti));

        blacklistService.blacklistToken(jti, "saket");

        assertTrue(blacklistService.isBlacklisted(jti));
    }

    @Test
    void blacklistToken_shouldStoreUsername() {
        JwtBlacklist saved = blacklistService.blacklistToken("jti-1", "alice");

        assertEquals("alice", saved.getUsername());
        assertEquals("alice", jwtBlacklistRepository.findByJti("jti-1").orElseThrow().getUsername());
    }

    @Test
    void blacklistToken_sameJtiTwice_shouldRemainBlacklisted() {
        // Behavior depends on repository semantics; this fake overwrites by jti in the map.
        blacklistService.blacklistToken("dup-jti", "user1");
        JwtBlacklist second = blacklistService.blacklistToken("dup-jti", "user2");

        assertTrue(blacklistService.isBlacklisted("dup-jti"));
        assertNotNull(second);
        assertEquals("dup-jti", second.getJti());
        assertEquals(1, jwtBlacklistRepository.count());
    }
}
