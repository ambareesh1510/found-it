package lost_and_found.app.auth.infra;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import io.jsonwebtoken.Jwts;
import lost_and_found.app.auth.service.AppleTokens;
import lost_and_found.app.shared.util.Csv;

/**
 * Calls Apple's {@code /auth/token} and {@code /auth/revoke} endpoints. Requests are authenticated with
 * a short-lived client secret: an ES256 JWT signed with the team's {@code .p8} private key.
 *
 * <p>Disabled until {@code APPLE_TEAM_ID}, {@code APPLE_KEY_ID}, and {@code APPLE_PRIVATE_KEY_PEM} are set.
 * A malformed key fails at startup rather than on the first sign-in.
 */
@Component
public class AppleTokenClient implements AppleTokens {

    static final String APPLE_BASE_URL = "https://appleid.apple.com";
    private static final Duration CLIENT_SECRET_LIFETIME = Duration.ofMinutes(5);
    private static final Logger log = LoggerFactory.getLogger(AppleTokenClient.class);

    private final RestClient restClient;
    private final String clientId;
    private final String teamId;
    private final String keyId;
    private final PrivateKey privateKey;

    @Autowired
    public AppleTokenClient(
        @Value("${app.apple.bundle-ids:}") String bundleIds,
        @Value("${app.apple.team-id:}") String teamId,
        @Value("${app.apple.key-id:}") String keyId,
        @Value("${app.apple.private-key-pem:}") String privateKeyPem
    ) {
        this(
            RestClient.builder().baseUrl(APPLE_BASE_URL).build(),
            Csv.split(bundleIds).stream().findFirst().orElse(null),
            teamId,
            keyId,
            privateKeyPem == null || privateKeyPem.isBlank() ? null : loadPrivateKey(privateKeyPem)
        );
    }

    /** @param clientId the app's bundle id (the client id Apple expects for native sign-in). */
    AppleTokenClient(RestClient restClient, String clientId, String teamId, String keyId, PrivateKey privateKey) {
        this.restClient = restClient;
        this.clientId = clientId;
        this.teamId = teamId == null ? "" : teamId.trim();
        this.keyId = keyId == null ? "" : keyId.trim();
        this.privateKey = privateKey;
    }

    boolean isConfigured() {
        return privateKey != null && clientId != null && !teamId.isEmpty() && !keyId.isEmpty();
    }

    @Override
    public Optional<String> exchangeAuthorizationCode(String authorizationCode) {
        if (!isConfigured()) {
            log.info("Apple authorization code not exchanged: Sign in with Apple is not configured.");
            return Optional.empty();
        }
        try {
            MultiValueMap<String, String> form = clientCredentials();
            form.add("grant_type", "authorization_code");
            form.add("code", authorizationCode);

            Map<String, Object> body = restClient.post()
                .uri("/auth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            Object refreshToken = body == null ? null : body.get("refresh_token");
            if (refreshToken == null) {
                log.warn("Apple token exchange returned no refresh token.");
                return Optional.empty();
            }
            return Optional.of(refreshToken.toString());
        } catch (RestClientException e) {
            log.warn("Apple token exchange failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void revokeRefreshToken(String refreshToken) {
        if (!isConfigured()) {
            log.warn("Apple sign-in was not revoked: Sign in with Apple is not configured on this server.");
            return;
        }
        try {
            MultiValueMap<String, String> form = clientCredentials();
            form.add("token", refreshToken);
            form.add("token_type_hint", "refresh_token");

            restClient.post()
                .uri("/auth/revoke")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Apple sign-in revocation failed: {}", e.getMessage());
        }
    }

    private MultiValueMap<String, String> clientCredentials() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", createClientSecret(Instant.now()));
        return form;
    }

    /** Builds the client secret Apple expects: ES256, {@code kid} = key id, {@code iss} = team id, {@code sub} = client id. */
    String createClientSecret(Instant now) {
        return Jwts.builder()
            .header().keyId(keyId).and()
            .issuer(teamId)
            .subject(clientId)
            .audience().add(APPLE_BASE_URL).and()
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(CLIENT_SECRET_LIFETIME)))
            .signWith(privateKey, Jwts.SIG.ES256)
            .compact();
    }

    private static PrivateKey loadPrivateKey(String pem) {
        try {
            // Accept real newlines or "\n" sequences, which is how multi-line values usually arrive in env vars.
            String normalized = pem.replace("\\n", "\n").trim();
            String header = "-----BEGIN PRIVATE KEY-----";
            String footer = "-----END PRIVATE KEY-----";
            int start = normalized.indexOf(header);
            int end = normalized.indexOf(footer);
            if (start < 0 || end < 0) {
                throw new IllegalArgumentException("expected a PKCS#8 PEM block");
            }
            byte[] der = Base64.getDecoder().decode(
                normalized.substring(start + header.length(), end).replaceAll("\\s", ""));
            return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("APPLE_PRIVATE_KEY_PEM is not a valid EC private key (.p8).", e);
        }
    }
}
