package lost_and_found.app.auth.service;

import java.util.Objects;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lost_and_found.app.auth.domain.AuthProviderType;
import lost_and_found.app.auth.dto.LoginResponseDTO;
import lost_and_found.app.exceptions.UserExistsException;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.infra.UserRepository;
import lost_and_found.app.shared.util.CookieUtils;

/**
 * A class that handles the logic for OAuth2 logins
 */
@Service
public class OAuth2AuthenticationService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthUtil authUtil;
    private static final String ACCESS_COOKIE_NAME = "access_token";

    private final long cookieExpireTimeSec;

    @Value("${security.jwt.is-prod}")
    boolean isProd;

    public OAuth2AuthenticationService(
        UserRepository userRepository,
        JwtService jwtService,
        AuthUtil authUtil,
        @Value("${security.jwt.expiration-time}") long cookieExpireTimeMs
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.authUtil = authUtil;
        this.cookieExpireTimeSec = cookieExpireTimeMs / 1000L;
    }

    /**
     * A function that handles the user registration, login and collision logic for OAuth2 logins.
     * @param oAuth2User The user info received from the OAuth2 provider (in this case, google).
     * @param registrationId The oauth2 provider.
     * @return A login response containing an access token and the username, plus the session cookie for the web app.
     * @throws IllegalStateException If the user is not formed by the end of the function.
     * @throws IllegalArgumentException if the user received from the oauth2 provider is not enough.
     * @throws UserExistsException if the user already exists and accounts cannot be combined.
     */
    @Transactional
    public ResponseEntity<LoginResponseDTO> handleOAuth2Login(OAuth2User oAuth2User, String registrationId)
        throws IllegalStateException, IllegalArgumentException, UserExistsException
    {
        AuthProviderType providerType = authUtil.getAuthProviderFromId(registrationId);
        String providerId = authUtil.getProviderId(oAuth2User, providerType);

        String email = oAuth2User.getAttribute("email");
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("OAuth provider did not supply an email");
        email = email.toLowerCase();

        User user = resolveSocialUser(providerType, providerId, email,
            () -> authUtil.determineUsernameFromOAuth(oAuth2User, providerId));

        LoginResponseDTO response = new LoginResponseDTO(
            jwtService.generateToken(user),
            user.getUsername()
        );

        ResponseCookie cookie = CookieUtils.addCookie(
            ACCESS_COOKIE_NAME,
            response.token(),
            cookieExpireTimeSec,
            true,
            isProd);

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(response);
    }

    /**
     * Finds or creates the local account for an identity verified by an OAuth provider.
     * Used by the browser OAuth flow and by the native Google and Apple sign-ins.
     *
     * @param providerType the provider that verified the identity.
     * @param providerId the provider's unique id for the user.
     * @param email the lower-cased email from the provider, or null if the provider did not send one.
     * @param usernameForNewUser builds a username if a new account has to be created.
     * @return the account to log in as.
     * @throws UserExistsException if the identity conflicts with another account.
     */
    @Transactional
    public User resolveSocialUser(
        AuthProviderType providerType,
        String providerId,
        String email,
        Supplier<String> usernameForNewUser
    ) throws UserExistsException {
        User user = userRepository.findByProviderIdAndAuthProviderType(providerId, providerType).orElse(null);

        if (email == null) {
            // Apple sends the email only on the first sign-in, so a returning user may come back without it.
            if (user == null) throw new IllegalArgumentException("OAuth provider did not supply an email");
            return user;
        }

        User emailUser = userRepository.findByEmail(email).orElse(null);

        // First time with this provider and this email: create the account.
        if (emailUser == null && user == null) {
            return registerWithOAuth(usernameForNewUser.get(), email, providerId, providerType);
        }

        // Two accounts: one with this provider identity, one with this email. Never merge them silently.
        if (user != null && emailUser != null) {
            if (!Objects.equals(emailUser.getId(), user.getId()))
                throw new UserExistsException("There are conflicts between an OAuth2 user and an email user on email " + email);
            return user;
        }

        // The provider identity is known; keep its email up to date.
        if (user != null) {
            if (user.getEmail() == null || !email.equalsIgnoreCase(user.getEmail())) {
                user.setEmail(email);
                return userRepository.save(user);
            }
            return user;
        }

        // An account with this email exists and this provider identity is new: link them.
        if (emailUser.getProviderId() != null)
            throw new UserExistsException("Email already linked to an OAuth provider");
        // Only link to a verified account. An unverified password account could have been created
        // by someone else using this email, and linking would hand them the new login.
        if (!emailUser.isEnabled())
            throw new UserExistsException("This email has a password account that is not verified yet. Verify it, then log in with your password.");
        emailUser.setProviderId(providerId);
        emailUser.setAuthProviderType(providerType);
        return userRepository.save(emailUser);
    }

    /**
     * A function that registers with OAuth2
     * @param username the username that will be stored in the database
     * @param email te email that will be stored in the database
     * @param providerId the unique id of the user'ss email from google
     * @param authProviderType the type of OAuth2 provider
     * @return a fully created user.
     * @throws IllegalArgumentException if any of the arguments are null
     * @throws UserExistsException if the user already exists.
     */
    private User registerWithOAuth(String username, String email, String providerId, AuthProviderType authProviderType)
        throws IllegalArgumentException, UserExistsException
    {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("username is null/blank");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("email is null/blank");
        if (providerId == null || providerId.isBlank()) throw new IllegalArgumentException("providerId is null/blank");
        if (authProviderType == null) throw new IllegalArgumentException("authProviderType is null");

        boolean userTaken = userRepository.existsByUsernameOrEmail(username, email);

        if (userTaken) throw new UserExistsException("Email or username already exists");

        User user = User.builder()
            .username(username)
            .password(null)
            .email(email)
            .authProviderType(authProviderType)
            .providerId(providerId)
            .enabled(true)
            .build();

        return userRepository.save(user);
    }
}
