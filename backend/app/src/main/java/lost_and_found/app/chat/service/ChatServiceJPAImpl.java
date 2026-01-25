package lost_and_found.app.chat.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lost_and_found.app.chat.domain.ChatMessage;
import lost_and_found.app.chat.domain.ChatThread;
import lost_and_found.app.chat.dto.requests.CreateChatThreadDTO;
import lost_and_found.app.chat.dto.requests.SendChatMessageDTO;
import lost_and_found.app.chat.dto.responses.ChatMessageResponseDTO;
import lost_and_found.app.chat.dto.responses.ChatThreadSummaryDTO;
import lost_and_found.app.chat.infra.ChatMessageRepository;
import lost_and_found.app.chat.infra.ChatThreadRepository;
import lost_and_found.app.chat.realtime.ChatRealtimeBroadcaster;
import lost_and_found.app.exceptions.ItemNotFoundException;
import lost_and_found.app.exceptions.UserMismatchException;
import lost_and_found.app.exceptions.UserNotFoundException;
import lost_and_found.app.items.domain.ItemPost;
import lost_and_found.app.items.infra.ItemPostRepository;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.infra.UserRepository;

@Service
public class ChatServiceJPAImpl implements ChatService {

    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ItemPostRepository itemPostRepository;
    private final UserRepository userRepository;
    private final ChatRealtimeBroadcaster chatRealtimeBroadcaster;

    public ChatServiceJPAImpl(
        ChatThreadRepository chatThreadRepository,
        ChatMessageRepository chatMessageRepository,
        ItemPostRepository itemPostRepository,
        UserRepository userRepository,
        ChatRealtimeBroadcaster chatRealtimeBroadcaster
    ) {
        this.chatThreadRepository = chatThreadRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.itemPostRepository = itemPostRepository;
        this.userRepository = userRepository;
        this.chatRealtimeBroadcaster = chatRealtimeBroadcaster;
    }

    @Transactional(readOnly = true)
    @Override
    public List<ChatThreadSummaryDTO> getThreadsForUser(Long userId) {
        return chatThreadRepository.findByUser1IdOrUser2IdOrderByUpdatedAtDesc(userId, userId)
            .stream()
            .sorted(Comparator.comparing(ChatThread::getUpdatedAt).reversed())
            .map(thread -> toSummary(thread, userId))
            .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<ChatThreadSummaryDTO> getThreadsForItem(Long userId, Long itemId) {
        ItemPost item = itemPostRepository.findById(itemId)
            .orElseThrow(() -> new ItemNotFoundException("Item " + itemId + " not found."));

        if (Long.compare(userId, item.getUser().getId()) != 0) {
            throw new UserMismatchException("You do not have access to view threads for this item.");
        }

        return chatThreadRepository.findByItemPostIdOrderByUpdatedAtDesc(item.getId())
            .stream()
            .sorted(Comparator.comparing(ChatThread::getUpdatedAt).reversed())
            .map(thread -> toSummary(thread, userId))
            .toList();
    }

    @Transactional
    @Override
    public ChatThreadSummaryDTO openThread(Long userId, CreateChatThreadDTO req) {
        ItemPost item = itemPostRepository.findById(req.itemId())
            .orElseThrow(() -> new ItemNotFoundException("Item " + req.itemId() + " not found."));

        User participant = userRepository.findById(req.participantUserId())
            .orElseThrow(() -> new UserNotFoundException("User " + req.participantUserId() + " not found."));

        if (Long.compare(userId, participant.getId()) == 0) {
            throw new UserMismatchException("You cannot start a chat with yourself.");
        }

        long firstUserId = Math.min(userId, participant.getId());
        long secondUserId = Math.max(userId, participant.getId());

        User firstUser = userRepository.findById(firstUserId)
            .orElseThrow(() -> new UserNotFoundException("User " + firstUserId + " not found."));
        User secondUser = userRepository.findById(secondUserId)
            .orElseThrow(() -> new UserNotFoundException("User " + secondUserId + " not found."));

        ChatThread thread = chatThreadRepository
            .findByItemPostIdAndUser1IdAndUser2Id(item.getId(), firstUserId, secondUserId)
            .orElseGet(() -> chatThreadRepository.save(new ChatThread(null, item, firstUser, secondUser, null, null)));

        return toSummary(thread, userId);
    }

    @Transactional
    @Override
    public ChatThreadSummaryDTO openThreadForItem(Long userId, Long itemId) {
        ItemPost item = itemPostRepository.findById(itemId)
            .orElseThrow(() -> new ItemNotFoundException("Item " + itemId + " not found."));
        return openThread(userId, new CreateChatThreadDTO(item.getId(), item.getUser().getId()));
    }

    @Transactional(readOnly = true)
    @Override
    public List<ChatMessageResponseDTO> getMessages(Long userId, Long threadId) {
        ChatThread thread = getThreadForUser(userId, threadId);

        return chatMessageRepository.findByThreadIdOrderBySentAtAsc(thread.getId())
            .stream()
            .map(message -> toMessageResponse(message, userId))
            .toList();
    }

    @Transactional
    @Override
    public ChatMessageResponseDTO sendMessage(Long userId, Long threadId, SendChatMessageDTO req) {
        ChatThread thread = getThreadForUser(userId, threadId);
        User sender = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User " + userId + " not found."));
        String trimmedMessage = req.messageText() == null ? "" : req.messageText().trim();

        if (trimmedMessage.isBlank()) {
            throw new IllegalArgumentException("Message text is required.");
        }

        ChatMessage message = new ChatMessage();
        message.setThread(thread);
        message.setSenderUser(sender);
        message.setMessageText(trimmedMessage);

        ChatMessage saved = chatMessageRepository.save(message);
        thread.setUpdatedAt(saved.getSentAt());
        chatThreadRepository.save(thread);
        chatRealtimeBroadcaster.broadcastNewMessage(thread, saved);

        return toMessageResponse(saved, userId);
    }

    private ChatThread getThreadForUser(Long userId, Long threadId) {
        ChatThread thread = chatThreadRepository.findById(threadId)
            .orElseThrow(() -> new UserMismatchException("Chat thread " + threadId + " not found."));

        if (Long.compare(userId, thread.getUser1().getId()) != 0 && Long.compare(userId, thread.getUser2().getId()) != 0) {
            throw new UserMismatchException("You do not have access to this chat thread.");
        }

        return thread;
    }

    private ChatThreadSummaryDTO toSummary(ChatThread thread, Long currentUserId) {
        boolean currentIsUser1 = Long.compare(thread.getUser1().getId(), currentUserId) == 0;
        User otherUser = currentIsUser1 ? thread.getUser2() : thread.getUser1();
        ChatMessage lastMessage = chatMessageRepository.findFirstByThreadIdOrderBySentAtDesc(thread.getId())
            .orElse(null);

        String otherUserPicture = otherUser.getPicture() != null ? otherUser.getPicture() 
            : "https://ui-avatars.com/api/?name=" + otherUser.getUsername() + "&background=random&color=fff&size=128";

        ItemPost item = thread.getItemPost();

        return new ChatThreadSummaryDTO(
            thread.getId(),
            item.getId(),
            item.getTitle(),
            item.getImageUrl(),
            item.getStatus(),
            item.getUser().getId(),
            item.getResolvedWithUser() == null ? null : item.getResolvedWithUser().getId(),
            otherUser.getId(),
            otherUser.getUsername(),
            otherUserPicture,
            lastMessage == null ? null : lastMessage.getMessageText(),
            lastMessage == null ? thread.getUpdatedAt() : lastMessage.getSentAt()
        );
    }

    private ChatMessageResponseDTO toMessageResponse(ChatMessage message, Long currentUserId) {
        return new ChatMessageResponseDTO(
            message.getId(),
            message.getSenderUser().getId(),
            message.getSenderUser().getUsername(),
            message.getMessageText(),
            message.getSentAt(),
            Long.compare(message.getSenderUser().getId(), currentUserId) == 0
        );
    }
}
