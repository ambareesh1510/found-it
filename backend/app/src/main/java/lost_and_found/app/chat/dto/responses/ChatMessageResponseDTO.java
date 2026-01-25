package lost_and_found.app.chat.dto.responses;

import java.time.Instant;

public record ChatMessageResponseDTO(
    Long messageId,
    Long senderId,
    String senderUsername,
    String messageText,
    Instant sentAt,
    boolean mine
) {
}
