package lost_and_found.app.chat.realtime;

import lost_and_found.app.chat.dto.responses.ChatMessageResponseDTO;
import lost_and_found.app.chat.dto.responses.ChatThreadSummaryDTO;

public record ChatSocketOutboundEvent(
    String type,
    Long threadId,
    ChatMessageResponseDTO message,
    ChatThreadSummaryDTO thread,
    String error
) {
}
