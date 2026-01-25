package lost_and_found.app.chat.realtime;

import java.security.Principal;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

public class ChatWebSocketHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(
        ServerHttpRequest request,
        WebSocketHandler wsHandler,
        Map<String, Object> attributes
    ) {
        Object userId = attributes.get("userId");
        Object username = attributes.get("username");

        if (!(userId instanceof Long) || !(username instanceof String)) {
            return null;
        }

        return new ChatWebSocketPrincipal((Long) userId, (String) username);
    }

    public record ChatWebSocketPrincipal(Long userId, String username) implements Principal {
        @Override
        public String getName() {
            return username;
        }
    }
}
