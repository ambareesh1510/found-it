package lost_and_found.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import lost_and_found.app.auth.service.HS256JwtService;
import lost_and_found.app.auth.service.JwtService;
import lost_and_found.app.test_implementations.BlacklistServiceTest;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class HS256JwtServiceTest {

    private JwtService jwtService;
    private UserDetails user;

    @BeforeEach
    void setUp() {
        // 32+ bytes key material (here: 64 bytes) then Base64 encode (matches your Decoders.BASE64 usage)
        byte[] keyBytes = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
                .getBytes(StandardCharsets.UTF_8);
        String secretKeyB64 = Base64.getEncoder().encodeToString(keyBytes);

        jwtService = new HS256JwtService(
            new BlacklistServiceTest(),
            secretKeyB64,
            60_000L
        );

        user = User.withUsername("saket")
                .password("ignored")
                .authorities("USER")
                .build();
    }

    @Test
    void generateToken_and_extractUsername_shouldWork() {
        String token = jwtService.generateToken(user);
        assertNotNull(token);
        assertFalse(token.isBlank());

        String username = jwtService.extractUsername(token);
        assertEquals("saket", username);
    }

    @Test
    void isTokenValid_shouldReturnTrue_forCorrectUser() {
        String token = jwtService.generateToken(user);
        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void isTokenValid_shouldReturnFalse_forDifferentUser() {
        String token = jwtService.generateToken(user);

        UserDetails other = User.withUsername("someoneElse")
                .password("ignored")
                .authorities("USER")
                .build();

        assertFalse(jwtService.isTokenValid(token, other));
    }

    @Test
    void isTokenValid_shouldReturnFalse_forTamperedToken() {
        String token = jwtService.generateToken(user);

        // Tamper payload by flipping one char in the middle segment
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);

        String tamperedPayload = parts[1].substring(0, parts[1].length() - 1) +
                (parts[1].endsWith("A") ? "B" : "A");

        String tampered = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertFalse(jwtService.isTokenValid(tampered, user));
    }

    @Test
    void isTokenValid_shouldReturnFalse_forExpiredToken() throws InterruptedException {
        // Set expiry to 1ms for this test
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 1L);

        String token = jwtService.generateToken(user);
        Thread.sleep(5);

        assertFalse(jwtService.isTokenValid(token, user));
    }
}

