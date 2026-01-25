package lost_and_found.app.searches.infra;

import org.springframework.data.jpa.repository.JpaRepository;

import lost_and_found.app.searches.domain.LostSearch;

public interface LostSearchRepository extends JpaRepository<LostSearch, Long> {

    /** Removes every search declared by the given user (used when the account is deleted). */
    void deleteByUser_Id(Long userId);
}
