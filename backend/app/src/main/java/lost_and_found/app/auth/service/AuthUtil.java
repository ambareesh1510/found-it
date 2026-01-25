package lost_and_found.app.auth.service;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import lost_and_found.app.auth.domain.AuthProviderType;
import lost_and_found.app.exceptions.UnsupportedOAuthTypeError;
import lost_and_found.app.shared.infra.UserRepository;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A utility that helps with OAuth2 logins
 */
@Component
@Slf4j
public class AuthUtil {

    private final UserRepository userRepository;

    public AuthUtil(
        UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    /**
     * Turns the string id of an OAuth2 provider into an Enum (AuthProviderType)
     * @param registrationId the string id of an OAuth2 provider. 
     * @return AuthProviderType
     */
    public AuthProviderType getAuthProviderFromId(String registrationId) {
        return switch(registrationId.toLowerCase()) {
            case "google" -> AuthProviderType.GOOGLE;
            default -> throw new UnsupportedOAuthTypeError("OAuth for type " + registrationId + " is not supported.");
        };
    } 

    /**
     * Gets the unique user id from the OAuth2 JWT. This comes from a foreign OAuth2 server.
     * @param user the user info from the OAuth2 server (in our case google)
     * @param providerType the type of the provider (add more if OAuth2 with github/twitter is needed)
     * @return the unique id of the user's oauth2 account.
     */
    public String getProviderId(OAuth2User user, AuthProviderType providerType) {
        String providerId = switch(providerType) {
            case AuthProviderType.GOOGLE -> user.getAttribute("sub");
            default -> {
                log.error("OAuth Provider type " + providerType.toString() + " is not supported");
                throw new UnsupportedOAuthTypeError("OAuth Provider type " + providerType.toString() + " is not supported");
            }
        };
        if (providerId == null || providerId.isBlank()) {
            log.error("OAuth Provider type " + providerType.toString() + "'s id could not be determined.");
            throw new IllegalArgumentException("OAuth Provider type " + providerType.toString() + "'s id could not be determined.");
        }
        return providerId;
    }

    /**
     * A function that builds usernames from scratch for the oauth2 user.
     * @param user the user info from the OAuth2 server (in our case google)
     * @param providerId the unique id of the user's oauth2 account.
     * @return the username of the user that will be stored in our db.
     */
    public String determineUsernameFromOAuth(OAuth2User user, String providerId) {
        return determineUsername(user.getAttribute("email"), providerId);
    }

    /**
     * Builds a username from an email address, falling back to the provider id when there is no email.
     * @param email the user's email, or null
     * @param providerId the unique id of the user's oauth2 account.
     * @return the username of the user that will be stored in our db.
     */
    public String determineUsername(String email, String providerId) {
        if (email == null || email.isBlank()) {
            return "user" + providerId;
        }
        String userBase = email.split("@", 2)[0];
        String base = userBase
            .replaceAll("[^a-z0-9_]", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_+|_+$", "");

        if (base.isBlank()) base = "user";
        if (base.length() > 15) base = base.substring(0, 15);

        // try base, then base_2, base_3, ...
        String candidate = base;
        int attempts = 0;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        while (userRepository.existsByUsername(candidate)) {
            // build a random numeric suffix like "_7" or "_4831"
            int digits = Math.min(10, 1 + attempts / 3); // grows slowly if we keep colliding
            StringBuilder sb = new StringBuilder("_");
            for (int d = 0; d < digits; d++) {
                sb.append(rnd.nextInt(10)); // 0..9
            }
            String suffix = sb.toString();

            int maxBaseLen = 20 - suffix.length();
            String trimmedBase = base.length() > maxBaseLen ? base.substring(0, maxBaseLen) : base;
            candidate = trimmedBase + suffix;

            attempts++;
        }
        return candidate;
    }
}
