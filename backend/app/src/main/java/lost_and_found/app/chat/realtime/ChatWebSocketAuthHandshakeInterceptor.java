package lost_and_found.app.chat.realtime;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import lost_and_found.app.auth.service.JwtService;
import lost_and_found.app.auth.service.WsTicketService;
import lost_and_found.app.shared.domain.User;

/**
 * Admits a WebSocket connection only with a valid ticket from POST /chat/ws-ticket, passed as
 * ?ticket=. Browsers and WKWebView cannot send an Authorization header on a WebSocket, and session
 * tokens are not accepted here, so the long-lived token never appears in a URL.
 */
@Component
public class ChatWebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public ChatWebSocketAuthHandshakeInterceptor(
        JwtService jwtService,
        UserDetailsService userDetailsService
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public boolean beforeHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Map<String, Object> attributes
    ) {
        String ticket = UriComponentsBuilder.fromUri(request.getURI()).build()
            .getQueryParams().getFirst("ticket");
        if (ticket == null || ticket.isBlank()) {
            return false;
        }

        try {
            if (!WsTicketService.isTicket(jwtService, ticket)) {
                return false;
            }

            String username = jwtService.extractUsername(ticket);
            if (username == null || username.isBlank()) {
                return false;
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (!jwtService.isTokenValid(ticket, userDetails) || !(userDetails instanceof User user)) {
                return false;
            }

            attributes.put("userId", user.getId());
            attributes.put("username", user.getUsername());
            return true;
        } catch (RuntimeException e) {
            // Malformed, expired, or unknown-user tickets all fail the handshake the same way.
            return false;
        }
    }

    @Override
    public void afterHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Exception exception
    ) {
    }
}
