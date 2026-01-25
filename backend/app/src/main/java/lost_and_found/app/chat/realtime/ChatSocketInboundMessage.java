package lost_and_found.app.chat.realtime;

public record ChatSocketInboundMessage(
    String type,
    Long threadId,
    String messageText
) {
}
