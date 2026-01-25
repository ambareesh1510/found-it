package lost_and_found.app.auth.service;

import org.springframework.stereotype.Service;

import lost_and_found.app.auth.domain.AuthProviderType;
import lost_and_found.app.auth.dto.LoginResponseDTO;
import lost_and_found.app.auth.service.SocialIdTokenVerifier.VerifiedIdentity;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.infra.UserRepository;

/**
 * Signs in people who authenticated with Google or Apple inside the native app. The app sends the
 * identity token it got from the provider; this service verifies it and returns a session token.
 */
@Service
public class SocialAuthService {

    private final SocialIdTokenVerifier verifier;
    private final OAuth2AuthenticationService accounts;
    private final AuthUtil authUtil;
    private final JwtService jwtService;
    private final AppleTokens appleTokens;
    private final UserRepository userRepository;

    public SocialAuthService(
        SocialIdTokenVerifier verifier,
        OAuth2AuthenticationService accounts,
        AuthUtil authUtil,
        JwtService jwtService,
        AppleTokens appleTokens,
        UserRepository userRepository
    ) {
        this.verifier = verifier;
        this.accounts = accounts;
        this.authUtil = authUtil;
        this.jwtService = jwtService;
        this.appleTokens = appleTokens;
        this.userRepository = userRepository;
    }

    public LoginResponseDTO loginWithGoogle(String idToken) {
        User user = resolve(AuthProviderType.GOOGLE, verifier.verifyGoogle(idToken));
        return respond(user);
    }

    /**
     * @param identityToken the Apple identity token (a JWT).
     * @param authorizationCode the one-time code from the same sign-in. It is exchanged for a refresh
     *        token that is kept so the Apple link can be revoked on account deletion. May be null.
     */
    public LoginResponseDTO loginWithApple(String identityToken, String authorizationCode) {
        User user = resolve(AuthProviderType.APPLE, verifier.verifyApple(identityToken));

        if (authorizationCode != null && !authorizationCode.isBlank()) {
            appleTokens.exchangeAuthorizationCode(authorizationCode).ifPresent(refreshToken -> {
                user.setAppleRefreshToken(refreshToken);
                userRepository.save(user);
            });
        }
        return respond(user);
    }

    private User resolve(AuthProviderType providerType, VerifiedIdentity identity) {
        return accounts.resolveSocialUser(
            providerType,
            identity.providerId(),
            identity.email(),
            () -> authUtil.determineUsername(identity.email(), identity.providerId())
        );
    }

    private LoginResponseDTO respond(User user) {
        return new LoginResponseDTO(jwtService.generateToken(user), user.getUsername());
    }
}
