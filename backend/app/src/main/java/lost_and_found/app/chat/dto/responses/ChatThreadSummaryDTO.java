package lost_and_found.app.chat.dto.responses;

import java.time.Instant;

import lost_and_found.app.items.domain.ItemPostStatus;

public record ChatThreadSummaryDTO(
    Long threadId,
    Long itemId,
    String itemTitle,
    String itemImageUrl,
    ItemPostStatus itemStatus,
    Long itemPosterId,
    Long resolvedWithUserId,
    Long otherUserId,
    String otherUsername,
    String otherUserPicture,
    String lastMessage,
    Instant lastMessageAt
) {
}
