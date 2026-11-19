package lost_and_found.app.shared.infra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import lost_and_found.app.shared.domain.UserSettings;

/** Stores the per-user Settings toggles, keyed by user id. */
@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
}
