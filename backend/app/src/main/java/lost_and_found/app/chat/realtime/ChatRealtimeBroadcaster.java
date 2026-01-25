package lost_and_found.app.chat.realtime;

import java.io.IOException;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import lost_and_found.app.chat.domain.ChatMessage;
import lost_and_found.app.chat.domain.ChatThread;
import lost_and_found.app.chat.dto.responses.ChatMessageResponseDTO;
import lost_and_found.app.chat.dto.responses.ChatThreadSummaryDTO;
import lost_and_found.app.shared.domain.User;

@Component
public class ChatRealtimeBroadcaster {

    private final ChatRealtimeSessionRegistry sessionRegistry;
    private final ChatRealtimeJsonCodec jsonCodec;

    public ChatRealtimeBroadcaster(
        ChatRealtimeSessionRegistry sessionRegistry,
        ChatRealtimeJsonCodec jsonCodec
    ) {
        this.sessionRegistry = sessionRegistry;
        this.jsonCodec = jsonCodec;
    }

    public void broadcastNewMessage(ChatThread thread, ChatMessage message) {
        sendThreadEvent(thread.getUser1(), thread, message);
        sendThreadEvent(thread.getUser2(), thread, message);
    }

    public void sendError(Long userId, Long threadId, String error) {
        ChatSocketOutboundEvent event = new ChatSocketOutboundEvent(
            "chat_error",
            threadId,
            null,
            null,
            error
        );
        sendToUser(userId, event);
    }

    private void sendThreadEvent(User viewer, ChatThread thread, ChatMessage message) {
        User otherUser = thread.getUser1().getId().equals(viewer.getId()) ? thread.getUser2() : thread.getUser1();
        String otherUserPicture = otherUser.getPicture() != null ? otherUser.getPicture()
            : "https://ui-avatars.com/api/?name=" + otherUser.getUsername() + "&background=random&color=fff&size=128";

        ChatMessageResponseDTO messageDto = new ChatMessageResponseDTO(
            message.getId(),
            message.getSenderUser().getId(),
            message.getSenderUser().getUsername(),
            message.getMessageText(),
            message.getSentAt(),
            message.getSenderUser().getId().equals(viewer.getId())
        );

        ChatThreadSummaryDTO threadDto = new ChatThreadSummaryDTO(
            thread.getId(),
            thread.getItemPost().getId(),
            thread.getItemPost().getTitle(),
            thread.getItemPost().getImageUrl(),
            thread.getItemPost().getStatus(),
            thread.getItemPost().getUser().getId(),
            thread.getItemPost().getResolvedWithUser() == null ? null : thread.getItemPost().getResolvedWithUser().getId(),
            otherUser.getId(),
            otherUser.getUsername(),
            otherUserPicture,
            message.getMessageText(),
            message.getSentAt()
        );

        ChatSocketOutboundEvent event = new ChatSocketOutboundEvent(
            "chat_message",
            thread.getId(),
            messageDto,
            threadDto,
            null
        );

        sendToUser(viewer.getId(), event);
    }

    private void sendToUser(Long userId, ChatSocketOutboundEvent event) {
        String payload = jsonCodec.writeOutboundEvent(event);
        Set<WebSocketSession> sessions = sessionRegistry.getSessions(userId);

        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) continue;

            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(payload));
                }
            } catch (IOException ignored) {
            }
        }
    }
}
