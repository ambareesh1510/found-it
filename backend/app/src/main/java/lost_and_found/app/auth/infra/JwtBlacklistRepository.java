package lost_and_found.app.auth.infra;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import lost_and_found.app.auth.domain.JwtBlacklist;

@Repository
public interface JwtBlacklistRepository extends JpaRepository<JwtBlacklist, Long> {

    boolean existsByJti(String jti);

    Optional<JwtBlacklist> findByJti(String jti);
}
