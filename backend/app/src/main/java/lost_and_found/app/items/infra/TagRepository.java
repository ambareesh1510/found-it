package lost_and_found.app.items.infra;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import lost_and_found.app.items.domain.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByNameIgnoreCase(String name);

    List<Tag> findByIsActiveTrue();

    List<Tag> findByCategoryIgnoreCase(String category);
}
