package lost_and_found.app.chat.realtime;

import java.time.Instant;
import java.util.Map;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.json.JsonWriter;
import org.springframework.stereotype.Component;
import lost_and_found.app.chat.dto.responses.ChatMessageResponseDTO;
import lost_and_found.app.chat.dto.responses.ChatThreadSummaryDTO;

@Component
public class ChatRealtimeJsonCodec {

    private static final JsonWriter<ChatSocketOutboundEvent> EVENT_WRITER = JsonWriter.of(members -> {
        members.add("type", ChatSocketOutboundEvent::type);
        members.add("threadId", ChatSocketOutboundEvent::threadId);
        members.add("message", ChatSocketOutboundEvent::message).whenNotNull().usingMembers(messageMembers -> {
            messageMembers.add("messageId", ChatMessageResponseDTO::messageId);
            messageMembers.add("senderId", ChatMessageResponseDTO::senderId);
            messageMembers.add("senderUsername", ChatMessageResponseDTO::senderUsername);
            messageMembers.add("messageText", ChatMessageResponseDTO::messageText);
            messageMembers.add("sentAt", message -> formatInstant(message.sentAt()));
            messageMembers.add("mine", ChatMessageResponseDTO::mine);
        });
        members.add("thread", ChatSocketOutboundEvent::thread).whenNotNull().usingMembers(threadMembers -> {
            threadMembers.add("threadId", ChatThreadSummaryDTO::threadId);
            threadMembers.add("itemId", ChatThreadSummaryDTO::itemId);
            threadMembers.add("itemTitle", ChatThreadSummaryDTO::itemTitle);
            threadMembers.add("itemImageUrl", ChatThreadSummaryDTO::itemImageUrl);
            threadMembers.add("otherUserId", ChatThreadSummaryDTO::otherUserId);
            threadMembers.add("otherUsername", ChatThreadSummaryDTO::otherUsername);
            threadMembers.add("otherUserPicture", ChatThreadSummaryDTO::otherUserPicture);
            threadMembers.add("lastMessage", ChatThreadSummaryDTO::lastMessage);
            threadMembers.add("lastMessageAt", thread -> formatInstant(thread.lastMessageAt()));
        });
        members.add("error", ChatSocketOutboundEvent::error);
    });

    private final JsonParser jsonParser;

    public ChatRealtimeJsonCodec() {
        this.jsonParser = JsonParserFactory.getJsonParser();
    }

    public ChatSocketInboundMessage readInboundMessage(String payload) {
        try {
            Map<String, Object> json = jsonParser.parseMap(payload);
            return new ChatSocketInboundMessage(
                asString(json.get("type")),
                asLong(json.get("threadId")),
                asString(json.get("messageText"))
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to parse chat websocket payload.", e);
        }
    }

    public String writeOutboundEvent(ChatSocketOutboundEvent event) {
        return EVENT_WRITER.writeToString(event);
    }

    private static Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private static String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    private static String formatInstant(Instant instant) {
        return instant != null ? instant.toString() : null;
    }
}
