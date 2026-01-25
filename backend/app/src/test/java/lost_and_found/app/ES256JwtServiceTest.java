package lost_and_found.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import lost_and_found.app.auth.service.ES256JwtService;
import lost_and_found.app.auth.service.JwtService;
import lost_and_found.app.test_implementations.BlacklistServiceTest;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class ES256JwtServiceTest {

    private JwtService jwtService;
    private UserDetails user;

    @BeforeEach
    void setUp() throws Exception {

        KeyPair kp = generateP256KeyPair();
        String privatePem = toPkcs8PrivatePem((ECPrivateKey) kp.getPrivate());
        String publicPem = toX509PublicPem((ECPublicKey) kp.getPublic());
        jwtService = new ES256JwtService(
            new BlacklistServiceTest(),
            privatePem,
            publicPem,
            100000
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

        assertEquals("saket", jwtService.extractUsername(token));
    }

    @Test
    void isTokenValid_shouldReturnTrue_forCorrectUser() {
        String token = jwtService.generateToken(user);
        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void isTokenValid_shouldReturnFalse_forTamperedToken() {
        String token = jwtService.generateToken(user);

        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);

        String tamperedPayload = parts[1].substring(0, parts[1].length() - 1) +
                (parts[1].endsWith("A") ? "B" : "A");

        String tampered = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertFalse(jwtService.isTokenValid(tampered, user));
    }

    @Test
    void isTokenValid_shouldReturnFalse_forExpiredToken() throws InterruptedException {
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 1L);

        String token = jwtService.generateToken(user);
        Thread.sleep(5);

        assertFalse(jwtService.isTokenValid(token, user));
    }

    // --- helpers ---

    private static KeyPair generateP256KeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        // P-256 / secp256r1 is the curve used by ES256
        kpg.initialize(256);
        return kpg.generateKeyPair();
    }

    private static String toPkcs8PrivatePem(ECPrivateKey privateKey) {
        String b64 = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(privateKey.getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + b64 + "\n-----END PRIVATE KEY-----";
    }

    private static String toX509PublicPem(ECPublicKey publicKey) {
        String b64 = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(publicKey.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + b64 + "\n-----END PUBLIC KEY-----";
    }
}

