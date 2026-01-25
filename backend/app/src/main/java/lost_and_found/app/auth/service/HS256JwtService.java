package lost_and_found.app.auth.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Profile("dev")
@Service
public class HS256JwtService implements JwtService {
    
    private final String secretKeyB64;

    private final long jwtExpirationMs;

    // Cache the derived key so we don't decode Base64 every call
    private volatile SecretKey cachedKey;

    private final BlacklistService blacklistService;

    public HS256JwtService(
        BlacklistService blacklistService,
        @Value("${security.jwt.hmac-secret-key}") String secretKeyB64,
        @Value("${security.jwt.expiration-time}") long jwtExpirationMs
    ) {
        this.blacklistService = blacklistService;
        this.secretKeyB64 = secretKeyB64;
        this.jwtExpirationMs = jwtExpirationMs;
    }

    @Override
    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }
    
    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    @Override
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    @Override
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpirationMs);
    }

    @Override
    public String generateShortLivedToken(Map<String, Object> extraClaims, UserDetails userDetails, long ttlMs) {
        return buildToken(extraClaims, userDetails, ttlMs);
    }

    @Override
    public long getExpirationTime() {
        return jwtExpirationMs;
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expirationMs) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        String id = UUID.randomUUID().toString();

        return Jwts.builder()
                .id(id)
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(exp)
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            final String jti = extractJti(token);

            return username.equals(userDetails.getUsername()) &&
                    !isTokenExpired(token) &&
                    !blacklistService.isBlacklisted(jti);
        } catch (JwtException | IllegalArgumentException e) {
            // invalid signature, malformed, expired, etc.
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        // v0.13 parsing: verifyWith + parseSignedClaims + getPayload
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSecretKey() {
        // Double-checked locking cache
        SecretKey k = cachedKey;
        if (k == null) {
            synchronized (this) {
                k = cachedKey;
                if (k == null) {
                    byte[] keyBytes = Decoders.BASE64.decode(secretKeyB64);
                    k = Keys.hmacShaKeyFor(keyBytes);
                    cachedKey = k;
                }
            }
        }
        return k;
    }
}
