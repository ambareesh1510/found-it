package lost_and_found.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import lost_and_found.app.auth.domain.AuthProviderType;
import lost_and_found.app.auth.dto.LoginResponseDTO;
import lost_and_found.app.auth.service.AuthUtil;
import lost_and_found.app.auth.service.HS256JwtService;
import lost_and_found.app.auth.service.JwtService;
import lost_and_found.app.auth.service.OAuth2AuthenticationService;
import lost_and_found.app.auth.service.SocialAuthService;
import lost_and_found.app.auth.service.SocialIdTokenVerifier;
import lost_and_found.app.exceptions.UserExistsException;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.test_implementations.BlacklistServiceTest;
import lost_and_found.app.test_implementations.FakeAppleTokens;
import lost_and_found.app.test_implementations.TestIdTokens;
import lost_and_found.app.test_implementations.UserRepositoryTest;

class SocialAuthServiceTest {

    static final String GOOGLE_ISS = "https://accounts.google.com";
    static final String APPLE_ISS = "https://appleid.apple.com";
    static final String WEB_CLIENT = "web-client.apps.googleusercontent.com";
    static final String IOS_CLIENT = "ios-client.apps.googleusercontent.com";
    static final String APP_BUNDLE = "edu.test.lostandfound";

    private UserRepositoryTest users;
    private JwtService jwtService;
    private KeyPair googleKeys;
    private KeyPair appleKeys;
    private SocialAuthService service;
    private FakeAppleTokens appleTokens;

    @BeforeEach
    void setUp() {
        String secret = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
        users = new UserRepositoryTest();
        jwtService = new HS256JwtService(new BlacklistServiceTest(), secret, 60_000L);
        AuthUtil authUtil = new AuthUtil(users);
        OAuth2AuthenticationService accounts = new OAuth2AuthenticationService(users, jwtService, authUtil, 60_000L);

        googleKeys = TestIdTokens.newRsaKeyPair();
        appleKeys = TestIdTokens.newRsaKeyPair();
        SocialIdTokenVerifier verifier = new SocialIdTokenVerifier(
            TestIdTokens.decoderFor(googleKeys),
            TestIdTokens.decoderFor(appleKeys),
            Set.of(WEB_CLIENT, IOS_CLIENT),
            Set.of(APP_BUNDLE));
        appleTokens = new FakeAppleTokens();
        service = new SocialAuthService(verifier, accounts, authUtil, jwtService, appleTokens, users);
    }

    @Test
    void aFirstGoogleSignInCreatesALinkedAccount() {
        String token = googleToken("google-sub-1", "Alice@Gmail.com", IOS_CLIENT, GOOGLE_ISS, true);

        LoginResponseDTO login = service.loginWithGoogle(token);

        assertNotNull(login.token());
        User user = users.findByProviderIdAndAuthProviderType("google-sub-1", AuthProviderType.GOOGLE).orElseThrow();
        assertEquals("alice@gmail.com", user.getEmail());
        assertEquals(login.username(), user.getUsername());
        assertEquals(user.getUsername(), jwtService.extractUsername(login.token()));
    }

    @Test
    void aReturningGoogleUserIsRecognisedEvenWithoutAnEmailClaim() {
        LoginResponseDTO first = service.loginWithGoogle(
            googleToken("google-sub-2", "bob@gmail.com", WEB_CLIENT, GOOGLE_ISS, true));
        LoginResponseDTO second = service.loginWithGoogle(
            googleToken("google-sub-2", null, WEB_CLIENT, GOOGLE_ISS, true));

        assertEquals(first.username(), second.username());
    }

    @Test
    void anAppleSignInCreatesAnAppleAccount() {
        String token = TestIdTokens.sign(appleKeys, APPLE_ISS, APP_BUNDLE, "apple-sub-1", in(10),
            Map.of("email", "carol@privaterelay.appleid.com", "email_verified", "true"));

        service.loginWithApple(token, null);

        User user = users.findByProviderIdAndAuthProviderType("apple-sub-1", AuthProviderType.APPLE).orElseThrow();
        assertEquals("carol@privaterelay.appleid.com", user.getEmail());
    }

    @Test
    void anAppleSignInKeepsTheRefreshTokenFromItsAuthorizationCode() {
        String token = TestIdTokens.sign(appleKeys, APPLE_ISS, APP_BUNDLE, "apple-sub-3", in(10),
            Map.of("email", "frank@privaterelay.appleid.com", "email_verified", "true"));

        service.loginWithApple(token, "one-time-code");

        assertEquals(List.of("one-time-code"), appleTokens.exchangedCodes);
        User user = users.findByProviderIdAndAuthProviderType("apple-sub-3", AuthProviderType.APPLE).orElseThrow();
        assertEquals("apple-refresh-token-1", user.getAppleRefreshToken());
    }

    @Test
    void anAppleSignInWithoutACodeStillWorksAndStoresNoRefreshToken() {
        String token = TestIdTokens.sign(appleKeys, APPLE_ISS, APP_BUNDLE, "apple-sub-4", in(10),
            Map.of("email", "gina@privaterelay.appleid.com", "email_verified", "true"));

        LoginResponseDTO login = service.loginWithApple(token, null);

        assertNotNull(login.token());
        assertEquals(List.of(), appleTokens.exchangedCodes);
        User user = users.findByProviderIdAndAuthProviderType("apple-sub-4", AuthProviderType.APPLE).orElseThrow();
        assertNull(user.getAppleRefreshToken());
    }

    @Test
    void anAppleSignInSucceedsEvenWhenAppleCannotIssueARefreshToken() {
        appleTokens.nextRefreshToken = java.util.Optional.empty();
        String token = TestIdTokens.sign(appleKeys, APPLE_ISS, APP_BUNDLE, "apple-sub-5", in(10),
            Map.of("email", "hal@privaterelay.appleid.com", "email_verified", "true"));

        LoginResponseDTO login = service.loginWithApple(token, "expired-code");

        assertNotNull(login.token());
        User user = users.findByProviderIdAndAuthProviderType("apple-sub-5", AuthProviderType.APPLE).orElseThrow();
        assertNull(user.getAppleRefreshToken());
    }

    @Test
    void anInvalidAppleTokenDoesNotSpendTheAuthorizationCode() {
        assertThrows(BadCredentialsException.class, () -> service.loginWithApple("not-a-token", "code-1"));

        assertEquals(List.of(), appleTokens.exchangedCodes);
    }

    @Test
    void aGoogleSignInLinksAVerifiedPasswordAccountWithTheSameEmail() {
        User existing = users.save(User.builder()
            .username("dave").email("dave@gmail.com").password("hash").enabled(true).build());

        LoginResponseDTO login = service.loginWithGoogle(
            googleToken("google-sub-3", "dave@gmail.com", WEB_CLIENT, GOOGLE_ISS, true));

        assertEquals("dave", login.username());
        User linked = users.findById(existing.getId()).orElseThrow();
        assertEquals("google-sub-3", linked.getProviderId());
    }

    @Test
    void aGoogleSignInDoesNotLinkAnUnverifiedPasswordAccount() {
        users.save(User.builder()
            .username("erin").email("erin@gmail.com").password("hash").enabled(false).build());

        assertThrows(UserExistsException.class, () ->
            service.loginWithGoogle(googleToken("google-sub-4", "erin@gmail.com", WEB_CLIENT, GOOGLE_ISS, true)));
    }

    @Test
    void aTokenIssuedToAnotherAppIsRejected() {
        String token = googleToken("google-sub-5", "x@gmail.com", "someone-elses-app", GOOGLE_ISS, true);

        assertThrows(BadCredentialsException.class, () -> service.loginWithGoogle(token));
    }

    @Test
    void aTokenFromAnUnknownIssuerIsRejected() {
        String token = googleToken("google-sub-6", "x@gmail.com", WEB_CLIENT, "https://evil.example", true);

        assertThrows(BadCredentialsException.class, () -> service.loginWithGoogle(token));
    }

    @Test
    void anExpiredTokenIsRejected() {
        String token = TestIdTokens.sign(googleKeys, GOOGLE_ISS, WEB_CLIENT, "google-sub-7", in(-60),
            Map.of("email", "x@gmail.com", "email_verified", true));

        assertThrows(BadCredentialsException.class, () -> service.loginWithGoogle(token));
    }

    @Test
    void aTokenSignedByAnotherKeyIsRejected() {
        String token = TestIdTokens.sign(TestIdTokens.newRsaKeyPair(), GOOGLE_ISS, WEB_CLIENT, "google-sub-8",
            in(10), Map.of("email", "x@gmail.com", "email_verified", true));

        assertThrows(BadCredentialsException.class, () -> service.loginWithGoogle(token));
    }

    @Test
    void anUnverifiedEmailIsRejected() {
        String token = googleToken("google-sub-9", "x@gmail.com", WEB_CLIENT, GOOGLE_ISS, false);

        assertThrows(BadCredentialsException.class, () -> service.loginWithGoogle(token));
    }

    @Test
    void anAppleTokenCannotSignInThroughTheGoogleEndpoint() {
        String appleToken = TestIdTokens.sign(appleKeys, APPLE_ISS, APP_BUNDLE, "apple-sub-2", in(10),
            Map.of("email", "y@example.com", "email_verified", "true"));

        assertThrows(BadCredentialsException.class, () -> service.loginWithGoogle(appleToken));
    }

    private String googleToken(String sub, String email, String audience, String issuer, boolean verified) {
        Map<String, Object> claims = email == null
            ? Map.of()
            : Map.of("email", email, "email_verified", verified);
        return TestIdTokens.sign(googleKeys, issuer, audience, sub, in(10), claims);
    }

    private static Instant in(long seconds) {
        return Instant.now().plusSeconds(seconds);
    }
}
