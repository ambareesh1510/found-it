package lost_and_found.app.shared.util;

import java.util.Arrays;
import java.util.Optional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseCookie;

public final class CookieUtils {

    private CookieUtils() {}

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();

        return Arrays.stream(cookies)
            .filter(c -> name.equals(c.getName()))
            .findFirst();
    }

    public static ResponseCookie addCookie(
        String name,
        String value,
        long maxAgeSeconds,
        boolean httpOnly,
        boolean isProd
    ) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
            .httpOnly(httpOnly)
            .secure(isProd)
            .sameSite(isProd ? "None" : "Lax")
            .path("/")
            .maxAge(maxAgeSeconds)
            .build();

        return cookie;
    }

    public static ResponseCookie deleteCookie(String name, boolean isProd) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
            .httpOnly(true)
            .secure(isProd)
            .sameSite(isProd ? "None" : "Lax")
            .path("/")
            .maxAge(0)
            .build();

        return cookie;
    }
}