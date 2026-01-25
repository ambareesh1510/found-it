package lost_and_found.app.chat.realtime;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import lost_and_found.app.shared.util.Csv;

@Configuration
@EnableWebSocket
public class ChatWebSocketConfig implements WebSocketConfigurer {

    @Value("${hostname.prod}")
    private String prodHostname;

    @Value("${hostname.dev}")
    private String devHostname;

    @Value("${app.native-origins:}")
    private String nativeOrigins;

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ChatWebSocketAuthHandshakeInterceptor authHandshakeInterceptor;

    public ChatWebSocketConfig(
        ChatWebSocketHandler chatWebSocketHandler,
        ChatWebSocketAuthHandshakeInterceptor authHandshakeInterceptor
    ) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.authHandshakeInterceptor = authHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        List<String> origins = new ArrayList<>(List.of(devHostname, prodHostname));
        origins.addAll(Csv.split(nativeOrigins));

        registry.addHandler(chatWebSocketHandler, "/ws/chat")
            .addInterceptors(authHandshakeInterceptor)
            .setHandshakeHandler(new ChatWebSocketHandshakeHandler())
            .setAllowedOrigins(origins.toArray(String[]::new));
    }
}
