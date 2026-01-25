package lost_and_found.app.test_implementations;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/** Builds RS256 identity tokens signed with a test key, shaped like the ones Google and Apple issue. */
public final class TestIdTokens {

    private TestIdTokens() {}

    public static KeyPair newRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A decoder that trusts only the given key, the test stand-in for the provider's JWKS endpoint. */
    public static NimbusJwtDecoder decoderFor(KeyPair keys) {
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) keys.getPublic()).build();
    }

    /**
     * @param extraClaims additional claims, e.g. {@code email} and {@code email_verified}.
     */
    public static String sign(
        KeyPair keys,
        String issuer,
        String audience,
        String subject,
        Instant expiresAt,
        Map<String, Object> extraClaims
    ) {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
            .issuer(issuer)
            .audience(audience)
            .subject(subject)
            .issueTime(new Date())
            .expirationTime(Date.from(expiresAt));
        extraClaims.forEach(claims::claim);

        try {
            SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(),
                claims.build());
            jwt.sign(new RSASSASigner((RSAPrivateKey) keys.getPrivate()));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
