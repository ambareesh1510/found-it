package lost_and_found.app.shared.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.JwtException;
import lost_and_found.app.auth.service.AppleTokens;
import lost_and_found.app.auth.service.BlacklistService;
import lost_and_found.app.auth.service.JwtService;
import lost_and_found.app.exceptions.UserNotFoundException;
import lost_and_found.app.searches.infra.LostSearchRepository;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.infra.UserRepository;

/** Account-level operations that touch more than one feature's data. */
@Service
public class AccountService {

    private final UserRepository userRepository;
    private final LostSearchRepository lostSearchRepository;
    private final BlacklistService blacklistService;
    private final JwtService jwtService;
    private final AppleTokens appleTokens;

    public AccountService(
        UserRepository userRepository,
        LostSearchRepository lostSearchRepository,
        BlacklistService blacklistService,
        JwtService jwtService,
        AppleTokens appleTokens
    ) {
        this.userRepository = userRepository;
        this.lostSearchRepository = lostSearchRepository;
        this.blacklistService = blacklistService;
        this.jwtService = jwtService;
        this.appleTokens = appleTokens;
    }

    /**
     * Deletes the user's personal data. The account row stays, so the user's posts, claims, and chat
     * messages still point at a real record, but nothing in it identifies the person any more: the
     * username, email, password, provider link, and photo are replaced or cleared, and the account can
     * no longer log in. The user's declared searches (location and time) are deleted outright, the
     * session token used for the request is revoked, and any Sign in with Apple link is revoked at Apple.
     * Apple revocation is best-effort: if it fails, the deletion still goes ahead and the failure is logged.
     *
     * @param userId the user to delete (the signed-in user; the controller passes their own id).
     * @param currentToken the session token on the request, or null.
     */
    @Transactional
    public void deleteAccount(Long userId, String currentToken) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User " + userId + " not found"));

        lostSearchRepository.deleteByUser_Id(userId);
        revokeToken(currentToken, user.getUsername());
        revokeAppleLink(user);

        String placeholder = "deleted-" + userId;
        user.setUsername(placeholder);
        user.setEmail(placeholder + "@deleted.invalid");
        user.setPassword(null);
        user.setProviderId(null);
        user.setAuthProviderType(null);
        user.setPicture(null);
        user.setAppleRefreshToken(null);
        user.setVerificationCode(null);
        user.setVerificationExpireTime(null);
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.setEnabled(false);
        userRepository.save(user);
    }

    private void revokeAppleLink(User user) {
        String refreshToken = user.getAppleRefreshToken();
        if (refreshToken == null) return;
        appleTokens.revokeRefreshToken(refreshToken);
    }

    private void revokeToken(String token, String username) {
        if (token == null || token.isBlank()) return;
        try {
            String jti = jwtService.extractJti(token);
            if (jti != null) blacklistService.blacklistToken(jti, username);
        } catch (JwtException | IllegalArgumentException e) {
            // The token is already invalid or expired, so there is nothing to revoke.
        }
    }
}
