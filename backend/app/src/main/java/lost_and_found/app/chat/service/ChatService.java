package lost_and_found.app.chat.service;

import java.util.List;

import lost_and_found.app.chat.dto.requests.CreateChatThreadDTO;
import lost_and_found.app.chat.dto.requests.SendChatMessageDTO;
import lost_and_found.app.chat.dto.responses.ChatMessageResponseDTO;
import lost_and_found.app.chat.dto.responses.ChatThreadSummaryDTO;

public interface ChatService {
    List<ChatThreadSummaryDTO> getThreadsForUser(Long userId);

    List<ChatThreadSummaryDTO> getThreadsForItem(Long userId, Long itemId);

    ChatThreadSummaryDTO openThread(Long userId, CreateChatThreadDTO req);

    /**
     * Opens (or reuses) the thread between {@code userId} and the item's poster.
     * The poster is looked up on the server, so clients never send a poster id.
     */
    ChatThreadSummaryDTO openThreadForItem(Long userId, Long itemId);

    List<ChatMessageResponseDTO> getMessages(Long userId, Long threadId);

    ChatMessageResponseDTO sendMessage(Long userId, Long threadId, SendChatMessageDTO req);
}
