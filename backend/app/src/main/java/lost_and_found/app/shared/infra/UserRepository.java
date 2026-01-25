package lost_and_found.app.shared.infra;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import lost_and_found.app.auth.domain.AuthProviderType;
import lost_and_found.app.shared.domain.User;

/**
 * A repository that communicates with the users table of the database,
 * and manages the updates and getting of User objects.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByVerificationCode(String verificationCode);
    Optional<User> findByResetToken(String resetToken);
    /** Case-insensitive so accounts saved with mixed case are still found. */
    @Query("""
        select u
        from User u
        where lower(u.email) = lower(:email)
    """)
    Optional<User> findByEmail(@Param("email") String email);
    @Query("""
        select u
        from User u
        where lower(u.username) = lower(:id)
            or lower(u.email) = lower(:id)
    """)
    Optional<User> findByUsernameOrEmail(@Param("id") String usernameOrEmail);
    @Query("""
        select case when count(u) > 0 then true else false end
        from User u
        where lower(u.username) = lower(:username)
            or lower(u.email) = lower(:email)
    """)
    boolean existsByUsernameOrEmail(@Param("username") String username, @Param("email") String email);
    boolean existsByUsername(String username);
    Optional<User> findByProviderIdAndAuthProviderType(String providerId, AuthProviderType authProviderType);
}
