package lost_and_found.app.test_implementations;

import lost_and_found.app.auth.domain.JwtBlacklist;
import lost_and_found.app.auth.service.BlacklistService;

public class BlacklistServiceTest implements BlacklistService {
    @Override
    public boolean isBlacklisted(String jti) {
        return false;
    }

    @Override
    public JwtBlacklist blacklistToken(String jti, String username) {
        return JwtBlacklist.builder()
            .jti(jti)
            .username(username)
            .build();
    }
}
