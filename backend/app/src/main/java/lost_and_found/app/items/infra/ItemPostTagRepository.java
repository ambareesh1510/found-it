package lost_and_found.app.items.infra;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import lost_and_found.app.items.domain.ItemPostTag;
import lost_and_found.app.items.domain.ItemPostTagId;

public interface ItemPostTagRepository extends JpaRepository<ItemPostTag, ItemPostTagId> {

    List<ItemPostTag> findByItemPostId(Long itemPostId);

    List<ItemPostTag> findByTagId(Long tagId);

    boolean existsByItemPostIdAndTagId(Long itemPostId, Long tagId);

    void deleteByItemPostIdAndTagId(Long itemPostId, Long tagId);
}
