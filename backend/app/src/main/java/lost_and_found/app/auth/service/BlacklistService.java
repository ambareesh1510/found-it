package lost_and_found.app.auth.service;

import lost_and_found.app.auth.domain.JwtBlacklist;

public interface BlacklistService {
    /**
     * Checks if a certain token with id jti is blacklisted 
     * @param jti the jwt id to be checked
     * @return true if blacklisted else false
     */
    boolean isBlacklisted(String jti);
    /**
     * Blacklists a token given a username and jti
     * @param jti the jwt id to be blacklisted
     * @param username the username the jwt belongs to.
     * @return the jwtblacklist object
     */
    JwtBlacklist blacklistToken(String jti, String username);
}
