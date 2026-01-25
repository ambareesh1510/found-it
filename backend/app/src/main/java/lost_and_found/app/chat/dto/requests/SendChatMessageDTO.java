package lost_and_found.app.chat.dto.requests;

import jakarta.validation.constraints.NotBlank;

public record SendChatMessageDTO(
    @NotBlank(message = "Message text is required.")
    String messageText
) {
}
