package lost_and_found.app.shared.util;

import org.springframework.http.HttpHeaders;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Finds the session JWT on a request. Native apps send it as {@code Authorization: Bearer};
 * the web app sends the {@code access_token} cookie.
 */
public final class RequestTokens {

    public static final String ACCESS_COOKIE_NAME = "access_token";
    private static final String BEARER_PREFIX = "Bearer ";

    private RequestTokens() {}

    /**
     * @return the token from the Authorization header if present, otherwise from the cookie, otherwise null.
     */
    public static String resolve(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            if (!token.isEmpty()) return token;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (ACCESS_COOKIE_NAME.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}
