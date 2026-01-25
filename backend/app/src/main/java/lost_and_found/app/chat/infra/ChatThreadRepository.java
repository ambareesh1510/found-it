package lost_and_found.app.chat.infra;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import lost_and_found.app.chat.domain.ChatThread;

public interface ChatThreadRepository extends JpaRepository<ChatThread, Long> {
    Optional<ChatThread> findByItemPostIdAndUser1IdAndUser2Id(Long itemPostId, Long user1Id, Long user2Id);

    List<ChatThread> findByUser1IdOrUser2IdOrderByUpdatedAtDesc(Long user1Id, Long user2Id);

    List<ChatThread> findByItemPostIdOrderByUpdatedAtDesc(Long itemPostId);
}
