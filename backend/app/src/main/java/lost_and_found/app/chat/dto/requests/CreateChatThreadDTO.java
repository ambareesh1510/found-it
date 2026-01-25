package lost_and_found.app.chat.dto.requests;

import jakarta.validation.constraints.NotNull;

public record CreateChatThreadDTO(
    @NotNull(message = "Item ID is required.")
    Long itemId,

    @NotNull(message = "Participant user ID is required.")
    Long participantUserId
) {
}
