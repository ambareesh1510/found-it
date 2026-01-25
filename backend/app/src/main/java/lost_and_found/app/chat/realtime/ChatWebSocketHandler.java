package lost_and_found.app.chat.realtime;

import java.security.Principal;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import lost_and_found.app.chat.dto.requests.SendChatMessageDTO;
import lost_and_found.app.chat.service.ChatService;
import lost_and_found.app.chat.realtime.ChatWebSocketHandshakeHandler.ChatWebSocketPrincipal;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatRealtimeJsonCodec jsonCodec;
    private final ChatService chatService;
    private final ChatRealtimeSessionRegistry sessionRegistry;
    private final ChatRealtimeBroadcaster broadcaster;

    public ChatWebSocketHandler(
        ChatRealtimeJsonCodec jsonCodec,
        ChatService chatService,
        ChatRealtimeSessionRegistry sessionRegistry,
        ChatRealtimeBroadcaster broadcaster
    ) {
        this.jsonCodec = jsonCodec;
        this.chatService = chatService;
        this.sessionRegistry = sessionRegistry;
        this.broadcaster = broadcaster;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        ChatWebSocketPrincipal principal = getPrincipal(session.getPrincipal());
        sessionRegistry.register(principal.userId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        ChatWebSocketPrincipal principal = getPrincipal(session.getPrincipal());
        ChatSocketInboundMessage message = jsonCodec.readInboundMessage(textMessage.getPayload());

        if (!"send_message".equals(message.type()) || message.threadId() == null) {
            broadcaster.sendError(principal.userId(), null, "Unsupported websocket message.");
            return;
        }

        try {
            chatService.sendMessage(
                principal.userId(),
                message.threadId(),
                new SendChatMessageDTO(message.messageText())
            );
        } catch (RuntimeException e) {
            broadcaster.sendError(principal.userId(), message.threadId(), e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessionRegistry.unregister(session);
    }

    private ChatWebSocketPrincipal getPrincipal(Principal principal) {
        if (principal instanceof ChatWebSocketPrincipal chatPrincipal) {
            return chatPrincipal;
        }

        throw new IllegalStateException("WebSocket connection is missing an authenticated chat principal.");
    }
}
