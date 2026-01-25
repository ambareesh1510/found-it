package lost_and_found.app.auth.service;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import lost_and_found.app.shared.util.Csv;

/**
 * Verifies the identity tokens that the native apps get from Google and Apple. The signature is
 * checked against the provider's published keys, and the issuer, audience, and expiry are enforced.
 */
@Component
public class SocialIdTokenVerifier {

    static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    static final String APPLE_JWKS_URI = "https://appleid.apple.com/auth/keys";
    private static final Set<String> GOOGLE_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    /** A verified identity. {@code email} is null when the provider did not include one. */
    public record VerifiedIdentity(String providerId, String email) {}

    private final NimbusJwtDecoder googleDecoder;
    private final NimbusJwtDecoder appleDecoder;

    @Autowired
    public SocialIdTokenVerifier(
        @Value("${app.google.client-ids:}") String googleClientIds,
        @Value("${app.apple.bundle-ids:}") String appleBundleIds
    ) {
        this(
            NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWKS_URI).build(),
            NimbusJwtDecoder.withJwkSetUri(APPLE_JWKS_URI).build(),
            Set.copyOf(Csv.split(googleClientIds)),
            Set.copyOf(Csv.split(appleBundleIds))
        );
    }

    /**
     * @param googleAudiences the client ids this app's Google tokens may be issued to (web and iOS).
     * @param appleAudiences the app bundle ids Apple tokens may be issued to.
     */
    public SocialIdTokenVerifier(
        NimbusJwtDecoder googleDecoder,
        NimbusJwtDecoder appleDecoder,
        Set<String> googleAudiences,
        Set<String> appleAudiences
    ) {
        this.googleDecoder = configure(googleDecoder, GOOGLE_ISSUERS::contains, googleAudiences);
        this.appleDecoder = configure(appleDecoder, APPLE_ISSUER::equals, appleAudiences);
    }

    /**
     * @throws BadCredentialsException if the token is invalid, expired, for another app, or has an unverified email.
     */
    public VerifiedIdentity verifyGoogle(String idToken) {
        return verify(googleDecoder, idToken);
    }

    /**
     * @throws BadCredentialsException if the token is invalid, expired, for another app, or has an unverified email.
     */
    public VerifiedIdentity verifyApple(String identityToken) {
        return verify(appleDecoder, identityToken);
    }

    private static VerifiedIdentity verify(NimbusJwtDecoder decoder, String token) {
        Jwt jwt;
        try {
            jwt = decoder.decode(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Sign-in token is invalid.", e);
        }

        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new BadCredentialsException("Sign-in token has no subject.");
        }

        String email = jwt.getClaimAsString("email");
        if (email != null && email.isBlank()) email = null;
        if (email != null && !isEmailVerified(jwt)) {
            throw new BadCredentialsException("Sign-in email is not verified.");
        }
        return new VerifiedIdentity(subject, email == null ? null : email.toLowerCase());
    }

    /** Google sends a boolean; Apple sends the string "true". Accept either. */
    private static boolean isEmailVerified(Jwt jwt) {
        Object verified = jwt.getClaim("email_verified");
        return Boolean.TRUE.equals(verified) || "true".equalsIgnoreCase(String.valueOf(verified));
    }

    private static NimbusJwtDecoder configure(
        NimbusJwtDecoder decoder,
        Predicate<String> issuerAllowed,
        Set<String> audiences
    ) {
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(),
            jwt -> issuerAllowed.test(jwt.getClaimAsString("iss"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_issuer", "Unexpected token issuer.", null)),
            jwt -> jwt.getAudience().stream().anyMatch(audiences::contains)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_audience", "Token was not issued for this app.", null))
        ));
        return decoder;
    }
}
