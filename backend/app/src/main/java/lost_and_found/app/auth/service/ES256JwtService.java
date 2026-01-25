package lost_and_found.app.auth.service;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

@SuppressWarnings("DoubleCheckedLocking")
@Profile("prod")
@Service
public class ES256JwtService implements JwtService {

    private final String ecPrivateKeyPem;

    private final String ecPublicKeyPem;

    private final long jwtExpirationMs;

    // Cache keys after first parse
    private volatile PrivateKey privateKey;
    private volatile PublicKey publicKey;

    private final BlacklistService blacklistService;

    public ES256JwtService(
        BlacklistService blacklistService,
        @Value("${security.jwt.ec-private-key-pem}") String ecPrivateKeyPem,
        @Value("${security.jwt.ec-public-key-pem}") String ecPublicKeyPem,
        @Value("${security.jwt.expiration-time}") long jwtExpirationMs
    ) {
        this.blacklistService = blacklistService;
        this.ecPrivateKeyPem = ecPrivateKeyPem;
        this.ecPublicKeyPem = ecPublicKeyPem;
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
    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
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
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            final String jti = extractJti(token);
            return username.equals(userDetails.getUsername()) && 
                    !isTokenExpired(token) && 
                    !blacklistService.isBlacklisted(jti);
        } catch (JwtException e) {
            // signature invalid, malformed, expired, etc.
            return false;
        }
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
                .signWith(getPrivateKey(), Jwts.SIG.ES256)
                .compact();
    }

    private boolean isTokenExpired(String token) {
        Date exp = extractClaim(token, Claims::getExpiration);
        return exp.before(new Date());
    }

    private Claims extractAllClaims(String token) {
        // ES256 verification uses PUBLIC key
        return Jwts.parser()
                .verifyWith(getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private PrivateKey getPrivateKey() {
        if (privateKey == null) {
            synchronized (this) {
                if (privateKey == null) {
                    privateKey = loadEcPrivateKeyFromPem(ecPrivateKeyPem);
                }
            }
        }
        return privateKey;
    }

    private PublicKey getPublicKey() {
        if (publicKey == null) {
            synchronized (this) {
                if (publicKey == null) {
                    publicKey = loadEcPublicKeyFromPem(ecPublicKeyPem);
                }
            }
        }
        return publicKey;
    }

    private static PrivateKey loadEcPrivateKeyFromPem(String pem) {
        try {
            byte[] der = decodePem(pem, "PRIVATE KEY");
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            return KeyFactory.getInstance("EC").generatePrivate(spec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to load EC private key (PKCS#8 PEM).", e);
        }
    }

    private static PublicKey loadEcPublicKeyFromPem(String pem) {
        try {
            byte[] der = decodePem(pem, "PUBLIC KEY");
            X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
            return KeyFactory.getInstance("EC").generatePublic(spec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to load EC public key (X.509 PEM).", e);
        }
    }

    private static byte[] decodePem(String pem, String type) {
        // Accept either actual newlines or "\n" sequences (common in env vars)
        String normalized = pem.replace("\\n", "\n").trim();

        String header = "-----BEGIN " + type + "-----";
        String footer = "-----END " + type + "-----";

        int start = normalized.indexOf(header);
        int end = normalized.indexOf(footer);

        if (start < 0 || end < 0) {
            throw new IllegalArgumentException("PEM does not contain expected block: " + type);
        }

        String base64 = normalized
                .substring(start + header.length(), end)
                .replaceAll("\\s", ""); // remove newlines/spaces

        return Base64.getDecoder().decode(base64);
    }
}
