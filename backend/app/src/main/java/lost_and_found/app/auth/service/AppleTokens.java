package lost_and_found.app.auth.service;

import java.util.Optional;

/**
 * Talks to Apple's token endpoints for Sign in with Apple. Used to keep a refresh token for each
 * Apple account, so the sign-in can be revoked when the account is deleted (App Store requirement).
 *
 * <p>Both methods are best-effort: they log failures rather than throwing, so a sign-in or an account
 * deletion never fails because Apple is unreachable or not configured.
 */
public interface AppleTokens {

    /**
     * Exchanges the one-time authorization code from the native sign-in for a refresh token.
     *
     * @return the refresh token, or empty if Apple is not configured or the exchange failed.
     */
    Optional<String> exchangeAuthorizationCode(String authorizationCode);

    /** Revokes a refresh token at Apple, which ends the Sign in with Apple link for that user. */
    void revokeRefreshToken(String refreshToken);
}
