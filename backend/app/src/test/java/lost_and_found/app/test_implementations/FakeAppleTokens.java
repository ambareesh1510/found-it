package lost_and_found.app.test_implementations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lost_and_found.app.auth.service.AppleTokens;

/** Records Apple token calls instead of making HTTP requests. */
public class FakeAppleTokens implements AppleTokens {

    public final List<String> exchangedCodes = new ArrayList<>();
    public final List<String> revokedRefreshTokens = new ArrayList<>();

    /** What the next exchange returns. Set to Optional.empty() to simulate Apple being unavailable. */
    public Optional<String> nextRefreshToken = Optional.of("apple-refresh-token-1");

    @Override
    public Optional<String> exchangeAuthorizationCode(String authorizationCode) {
        exchangedCodes.add(authorizationCode);
        return nextRefreshToken;
    }

    @Override
    public void revokeRefreshToken(String refreshToken) {
        revokedRefreshTokens.add(refreshToken);
    }
}
