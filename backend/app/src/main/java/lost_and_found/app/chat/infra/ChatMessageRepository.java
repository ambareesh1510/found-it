package lost_and_found.app.chat.infra;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import lost_and_found.app.chat.domain.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByThreadIdOrderBySentAtAsc(Long threadId);

    Optional<ChatMessage> findFirstByThreadIdOrderBySentAtDesc(Long threadId);
}
