package lost_and_found.app.test_implementations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;

import java.util.function.Function;

import lost_and_found.app.auth.domain.AuthProviderType;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.infra.UserRepository;

public class UserRepositoryTest implements UserRepository {

    private final Map<Long, User> usersById = new HashMap<>();
    private long nextId = 1L;

    private UnsupportedOperationException unsupported(String methodName) {
        return new UnsupportedOperationException(
            "UserRepositoryTest stub: '" + methodName + "' was called, but this test double does not implement it."
        );
    }

    // -------------------------
    // Implemented methods (useful for auth tests)
    // -------------------------

    @Override
    public <S extends User> S save(S entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Cannot save null user");
        }

        Long id = entity.getId();
        if (id == null) {
            id = nextId++;
        }

        // Rebuild User because your entity uses @Getter + @Builder and may not have setters
        @SuppressWarnings("unchecked")
        S saved = (S) User.builder()
            .id(id)
            .username(entity.getUsername())
            .password(entity.getPassword())
            .email(entity.getEmail())
            .enabled(entity.isEnabled())
            .verificationCode(entity.getVerificationCode())
            .verificationExpireTime(entity.getVerificationExpireTime())
            .resetToken(entity.getResetToken())
            .resetTokenExpiry(entity.getResetTokenExpiry())
            .providerId(entity.getProviderId())
            .authProviderType(entity.getAuthProviderType())
            .picture(entity.getPicture())
            .appleRefreshToken(entity.getAppleRefreshToken())
            .build();

        usersById.put(id, saved);
        return saved;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null) return Optional.empty();

        return usersById.values().stream()
            .filter(u -> username.equals(u.getUsername()))
            .findFirst();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();

        return usersById.values().stream()
            .filter(u -> email.equals(u.getEmail()))
            .findFirst();
    }

    @Override
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        if (usernameOrEmail == null) return Optional.empty();

        return usersById.values().stream()
            .filter(u ->
                usernameOrEmail.equals(u.getUsername()) ||
                usernameOrEmail.equals(u.getEmail()))
            .findFirst();
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(usersById.get(id));
    }

    @Override
    public boolean existsById(Long id) {
        return usersById.containsKey(id);
    }

    @Override
    public long count() {
        return usersById.size();
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(usersById.values());
    }

    @Override
    public void deleteById(Long id) {
        usersById.remove(id);
    }

    @Override
    public void delete(User entity) {
        if (entity == null) return;
        if (entity.getId() != null) {
            usersById.remove(entity.getId());
            return;
        }

        // fallback delete by username/email match if no ID
        usersById.entrySet().removeIf(entry -> {
            User u = entry.getValue();
            boolean sameUsername = entity.getUsername() != null && entity.getUsername().equals(u.getUsername());
            boolean sameEmail = entity.getEmail() != null && entity.getEmail().equals(u.getEmail());
            return sameUsername || sameEmail;
        });
    }

    @Override
    public void deleteAll() {
        usersById.clear();
    }

    // Optional but often useful in auth registration tests
    @Override
    public List<User> findAllById(Iterable<Long> ids) {
        List<User> results = new ArrayList<>();
        for (Long id : ids) {
            User u = usersById.get(id);
            if (u != null) results.add(u);
        }
        return results;
    }

    @Override
    public Optional<User> findByVerificationCode(String verificationCode) {
        if (verificationCode == null) return Optional.empty();

        return usersById.values().stream()
            .filter(u -> verificationCode.equals(u.getVerificationCode()))
            .findFirst();
    }

    @Override
    public Optional<User> findByResetToken(String resetToken) {
        if (resetToken == null) return Optional.empty();

        return usersById.values().stream()
            .filter(u -> resetToken.equals(u.getResetToken()))
            .findFirst();
    }

    // -------------------------
    // Everything else: unsupported for this fake
    // -------------------------

    @Override
    public void flush() {
        throw unsupported("flush");
    }

    @Override
    public <S extends User> S saveAndFlush(S entity) {
        // Simple fake behavior: same as save
        return save(entity);
    }

    @Override
    public <S extends User> List<S> saveAllAndFlush(Iterable<S> entities) {
        List<S> saved = new ArrayList<>();
        for (S entity : entities) {
            saved.add(save(entity));
        }
        return saved;
    }

    @Override
    public void deleteAllInBatch(Iterable<User> entities) {
        throw unsupported("deleteAllInBatch(Iterable<User>)");
    }

    @Override
    public void deleteAllByIdInBatch(Iterable<Long> ids) {
        throw unsupported("deleteAllByIdInBatch");
    }

    @Override
    public void deleteAllInBatch() {
        throw unsupported("deleteAllInBatch()");
    }

    @Override
    public User getOne(Long id) {
        throw unsupported("getOne");
    }

    @Override
    public User getById(Long id) {
        throw unsupported("getById");
    }

    @Override
    public User getReferenceById(Long id) {
        throw unsupported("getReferenceById");
    }

    @Override
    public <S extends User> List<S> findAll(Example<S> example) {
        throw unsupported("findAll(Example)");
    }

    @Override
    public <S extends User> List<S> findAll(Example<S> example, Sort sort) {
        throw unsupported("findAll(Example, Sort)");
    }

    @Override
    public <S extends User> List<S> saveAll(Iterable<S> entities) {
        List<S> saved = new ArrayList<>();
        for (S entity : entities) {
            saved.add(save(entity));
        }
        return saved;
    }

    @Override
    public List<User> findAll(Sort sort) {
        throw unsupported("findAll(Sort)");
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        throw unsupported("findAll(Pageable)");
    }

    @Override
    public <S extends User> Optional<S> findOne(Example<S> example) {
        throw unsupported("findOne(Example)");
    }

    @Override
    public <S extends User> Page<S> findAll(Example<S> example, Pageable pageable) {
        throw unsupported("findAll(Example, Pageable)");
    }

    @Override
    public <S extends User> long count(Example<S> example) {
        throw unsupported("count(Example)");
    }

    @Override
    public <S extends User> boolean exists(Example<S> example) {
        throw unsupported("exists(Example)");
    }

    @Override
    public <S extends User, R> R findBy(Example<S> example, Function<FetchableFluentQuery<S>, R> queryFunction) {
        throw unsupported("findBy");
    }

    @Override
    public void deleteAllById(Iterable<? extends Long> ids) {
        for (Long id : ids) {
            usersById.remove(id);
        }
    }

    @Override
    public void deleteAll(Iterable<? extends User> entities) {
        for (User u : entities) {
            delete(u);
        }
    }

    @Override
    public boolean existsByUsernameOrEmail(String username, String email) {
        if (username == null || email == null) return false;

        return usersById.values().stream()
            .anyMatch(u ->
                username.equals(u.getUsername()) ||
                email.equals(u.getEmail()));
    }

    @Override
    public boolean existsByUsername(String username) {
        if (username == null) return false;

        return usersById.values().stream()
            .anyMatch(u -> username.equals(u.getUsername()));
    }

    @Override
    public Optional<User> findByProviderIdAndAuthProviderType(String providerId, AuthProviderType authProviderType) {
        if (providerId == null || authProviderType == null) return Optional.empty();

        return usersById.values().stream()
            .filter(u -> providerId.equals(u.getProviderId()) &&
                        authProviderType.equals(u.getAuthProviderType()))
            .findFirst();
    }
}