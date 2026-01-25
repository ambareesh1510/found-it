package lost_and_found.app.auth.service;

import java.util.Map;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Issues short-lived tickets for the chat WebSocket. Browsers and WKWebView cannot send an
 * Authorization header on a WebSocket, so the client exchanges its session token for a ticket
 * and passes the ticket in the URL. A ticket is marked by a token-type claim and is rejected
 * everywhere a normal session token is accepted.
 */
@Service
public class WsTicketService {

    public static final String TYPE_CLAIM = "typ";
    public static final String TICKET_TYPE = "ws";

    /** Long enough to open the socket, short enough that a leaked ticket is quickly useless. */
    static final long TICKET_TTL_MS = 60_000L;

    private final JwtService jwtService;

    public WsTicketService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public String issue(UserDetails user) {
        return jwtService.generateShortLivedToken(Map.of(TYPE_CLAIM, TICKET_TYPE), user, TICKET_TTL_MS);
    }

    /** @return true if the token is a WebSocket ticket rather than a session token. */
    public static boolean isTicket(JwtService jwtService, String token) {
        return TICKET_TYPE.equals(jwtService.extractClaim(token, claims -> claims.get(TYPE_CLAIM)));
    }
}
