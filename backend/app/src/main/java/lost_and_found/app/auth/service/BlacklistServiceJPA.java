package lost_and_found.app.auth.service;

import java.time.Instant;
import org.springframework.stereotype.Service;

import lost_and_found.app.auth.domain.JwtBlacklist;
import lost_and_found.app.auth.infra.JwtBlacklistRepository;

@Service
public class BlacklistServiceJPA implements BlacklistService {

    private final JwtBlacklistRepository jwtBlacklistRepository;

    public BlacklistServiceJPA(
        JwtBlacklistRepository jwtBlacklistRepository
    ) {
        this.jwtBlacklistRepository = jwtBlacklistRepository;
    }

    @Override
    public boolean isBlacklisted(String jti) {
        return jwtBlacklistRepository.findByJti(jti).isPresent();
    }

    @Override
    public JwtBlacklist blacklistToken(String jti, String username) {
        return jwtBlacklistRepository.save(
            JwtBlacklist.builder()
                .blacklistedAt(Instant.now())
                .jti(jti)
                .username(username)
                .build()
        );
    }
}
