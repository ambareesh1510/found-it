package lost_and_found.app.items.infra;

import java.util.List;

import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import lost_and_found.app.items.domain.ItemPost;
import lost_and_found.app.items.domain.ItemPostStatus;

public interface ItemPostRepository extends JpaRepository<ItemPost, Long>, JpaSpecificationExecutor<ItemPost> {

    List<ItemPost> findByUserId(Long userId);

    List<ItemPost> findByStatus(ItemPostStatus status);

    List<ItemPost> findByCategoryIgnoreCase(String category);

    @Query(value = """
        SELECT *
        FROM item_posts ip
        WHERE ST_DWithin(
            ip.location,
            :center,
            :radiusMeters
        )
        ORDER BY ST_Distance(ip.location, :center)
        """, nativeQuery = true)
    List<ItemPost> findNearby(
        @Param("center") Point center,
        @Param("radiusMeters") double radiusMeters
    );
}
