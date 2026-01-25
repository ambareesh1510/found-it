package lost_and_found.app.auth.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import io.jsonwebtoken.Jwts;

class AppleTokenClientTest {

    private static final String BASE = "https://appleid.apple.com";
    private static final String CLIENT_ID = "edu.test.lostandfound";
    private static final String TEAM_ID = "TEAM123456";
    private static final String KEY_ID = "KEY7890ABC";

    private KeyPair appleKey;
    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private AppleTokenClient client;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        appleKey = generator.generateKeyPair();

        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AppleTokenClient(builder.baseUrl(BASE).build(), CLIENT_ID, TEAM_ID, KEY_ID, appleKey.getPrivate());
    }

    @Test
    void theClientSecretIsAnEs256JwtFromTheTeamAndKey() {
        String secret = client.createClientSecret(Instant.now());

        var jws = Jwts.parser().verifyWith(appleKey.getPublic()).build().parseSignedClaims(secret);

        assertEquals(KEY_ID, jws.getHeader().getKeyId());
        assertEquals(TEAM_ID, jws.getPayload().getIssuer());
        assertEquals(CLIENT_ID, jws.getPayload().getSubject());
        assertTrue(jws.getPayload().getAudience().contains(BASE));
        assertTrue(jws.getPayload().getExpiration().toInstant().isAfter(Instant.now()));
    }

    @Test
    void aClientSecretSignedWithAnotherKeyDoesNotVerify() throws Exception {
        String secret = client.createClientSecret(Instant.now());
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        PublicKey other = generator.generateKeyPair().getPublic();

        assertThrows(io.jsonwebtoken.JwtException.class,
            () -> Jwts.parser().verifyWith(other).build().parseSignedClaims(secret));
    }

    @Test
    void exchangeAuthorizationCodePostsTheCodeAndReturnsTheRefreshToken() {
        server.expect(requestTo(BASE + "/auth/token"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(content().string(containsString("grant_type=authorization_code")))
            .andExpect(content().string(containsString("code=one-time-code")))
            .andExpect(content().string(containsString("client_id=edu.test.lostandfound")))
            .andExpect(content().string(containsString("client_secret=")))
            .andRespond(withSuccess("{\"access_token\":\"a\",\"refresh_token\":\"refresh-123\"}", MediaType.APPLICATION_JSON));

        assertEquals(Optional.of("refresh-123"), client.exchangeAuthorizationCode("one-time-code"));
        server.verify();
    }

    @Test
    void exchangeReturnsEmptyWhenAppleRefuses() {
        server.expect(requestTo(BASE + "/auth/token"))
            .andRespond(withServerError());

        assertEquals(Optional.empty(), client.exchangeAuthorizationCode("bad-code"));
    }

    @Test
    void revokePostsTheRefreshTokenWithAHint() {
        server.expect(requestTo(BASE + "/auth/revoke"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(containsString("token=refresh-123")))
            .andExpect(content().string(containsString("token_type_hint=refresh_token")))
            .andRespond(withSuccess());

        client.revokeRefreshToken("refresh-123");
        server.verify();
    }

    @Test
    void revokeFailureIsSwallowedSoDeletionCanProceed() {
        server.expect(requestTo(BASE + "/auth/revoke")).andRespond(withServerError());

        client.revokeRefreshToken("refresh-123");
        server.verify();
    }

    @Test
    void whenNotConfiguredNothingIsSentToApple() {
        AppleTokenClient unconfigured = new AppleTokenClient(
            builder.baseUrl(BASE).build(), CLIENT_ID, "", "", null);

        assertFalse(unconfigured.isConfigured());
        assertEquals(Optional.empty(), unconfigured.exchangeAuthorizationCode("code"));
        unconfigured.revokeRefreshToken("refresh-123");
        server.verify(); // no expectations were set, so any request would have failed the mock
    }
}
