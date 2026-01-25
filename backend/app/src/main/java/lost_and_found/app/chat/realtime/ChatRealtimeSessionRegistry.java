package lost_and_found.app.chat.realtime;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class ChatRealtimeSessionRegistry {

    private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessionsByUserId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> userIdBySessionId = new ConcurrentHashMap<>();

    public void register(Long userId, WebSocketSession session) {
        sessionsByUserId.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
        userIdBySessionId.put(session.getId(), userId);
    }

    public void unregister(WebSocketSession session) {
        Long userId = userIdBySessionId.remove(session.getId());
        if (userId == null) return;

        sessionsByUserId.computeIfPresent(userId, (ignored, sessions) -> {
            sessions.remove(session);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    public Set<WebSocketSession> getSessions(Long userId) {
        return sessionsByUserId.getOrDefault(userId, Set.of());
    }

    public void closeAllForUser(Long userId) {
        for (WebSocketSession session : getSessions(userId)) {
            try {
                session.close();
            } catch (IOException ignored) {
            }
        }
    }
}
