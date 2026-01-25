package lost_and_found.app.test_implementations;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;

import lost_and_found.app.auth.domain.JwtBlacklist;
import lost_and_found.app.auth.infra.JwtBlacklistRepository;

/**
 * In-memory test double for JwtBlacklistRepository.
 * Implements only the methods needed by BlacklistServiceJPA tests.
 */
public class JwtBlacklistRepositoryTest implements JwtBlacklistRepository {

    private final Map<String, JwtBlacklist> byJti = new HashMap<>();
    private long nextId = 1L;

    private UnsupportedOperationException unsupported(String method) {
        return new UnsupportedOperationException(
            "JwtBlacklistRepositoryTest stub: '" + method + "' is not implemented."
        );
    }

    @Override
    public Optional<JwtBlacklist> findByJti(String jti) {
        return Optional.ofNullable(byJti.get(jti));
    }

    @Override
    public <S extends JwtBlacklist> S save(S entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Cannot save null JwtBlacklist");
        }

        Long id = entity.getId();
        if (id == null) {
            id = nextId++;
        }

        // Rebuild via builder to avoid mutability assumptions
        @SuppressWarnings("unchecked")
        S saved = (S) JwtBlacklist.builder()
            .id(id)
            .jti(entity.getJti())
            .username(entity.getUsername())
            .blacklistedAt(entity.getBlacklistedAt() != null ? entity.getBlacklistedAt() : Instant.now())
            .build();

        byJti.put(saved.getJti(), saved);
        return saved;
    }

    // -------- Optional helpers for tests --------
    public int size() {
        return byJti.size();
    }

    public void clearStore() {
        byJti.clear();
    }

    // -------- Everything else unsupported --------
    // Add methods only if your compiler asks (depends on your JwtBlacklistRepository interface / Spring Data version)

    @Override
    public Optional<JwtBlacklist> findById(Long id) { throw unsupported("findById"); }

    @Override
    public boolean existsById(Long id) { throw unsupported("existsById"); }

    @Override
    public List<JwtBlacklist> findAll() { throw unsupported("findall"); }

    @Override
    public List<JwtBlacklist> findAllById(Iterable<Long> ids) { throw unsupported("findAllById"); }

    @Override
    public long count() { return byJti.size(); }

    @Override
    public void deleteById(Long id) { throw unsupported("deleteById"); }

    @Override
    public void delete(JwtBlacklist entity) {
        if (entity == null || entity.getJti() == null) return;
        byJti.remove(entity.getJti());
    }

    @Override
    public void deleteAllById(Iterable<? extends Long> ids) { throw unsupported("deleteAllById"); }

    @Override
    public void deleteAll(Iterable<? extends JwtBlacklist> entities) {
        for (JwtBlacklist e : entities) delete(e);
    }

    @Override
    public void deleteAll() { byJti.clear(); }

    @Override
    public void flush() {
        throw unsupported("Unimplemented method 'flush'");
    }

    @Override
    public <S extends JwtBlacklist> S saveAndFlush(S entity) {
        throw unsupported("Unimplemented method 'saveAndFlush'");
    }

    @Override
    public <S extends JwtBlacklist> List<S> saveAllAndFlush(Iterable<S> entities) {
        throw unsupported("Unimplemented method 'saveAllAndFlush'");
    }

    @Override
    public void deleteAllInBatch(Iterable<JwtBlacklist> entities) {
        throw unsupported("Unimplemented method 'deleteAllInBatch'");
    }

    @Override
    public void deleteAllByIdInBatch(Iterable<Long> ids) {
        throw unsupported("Unimplemented method 'deleteAllByIdInBatch'");
    }

    @Override
    public void deleteAllInBatch() {
        throw unsupported("Unimplemented method 'deleteAllInBatch'");
    }

    @Override
    public JwtBlacklist getOne(Long id) {
        throw unsupported("Unimplemented method 'getOne'");
    }

    @Override
    public JwtBlacklist getById(Long id) {
        throw unsupported("Unimplemented method 'getById'");
    }

    @Override
    public JwtBlacklist getReferenceById(Long id) {
        throw unsupported("Unimplemented method 'getReferenceById'");
    }

    @Override
    public <S extends JwtBlacklist> List<S> findAll(Example<S> example) {
        throw unsupported("Unimplemented method 'findAll'");
    }

    @Override
    public <S extends JwtBlacklist> List<S> findAll(Example<S> example, Sort sort) {
        throw unsupported("Unimplemented method 'findAll'");
    }

    @Override
    public <S extends JwtBlacklist> List<S> saveAll(Iterable<S> entities) {
        throw unsupported("Unimplemented method 'saveAll'");
    }

    @Override
    public List<JwtBlacklist> findAll(Sort sort) {
        throw unsupported("Unimplemented method 'findAll'");
    }

    @Override
    public Page<JwtBlacklist> findAll(Pageable pageable) {
        throw unsupported("Unimplemented method 'findAll'");
    }

    @Override
    public <S extends JwtBlacklist> Optional<S> findOne(Example<S> example) {
        throw unsupported("Unimplemented method 'findOne'");
    }

    @Override
    public <S extends JwtBlacklist> Page<S> findAll(Example<S> example, Pageable pageable) {
        throw unsupported("Unimplemented method 'findAll'");
    }

    @Override
    public <S extends JwtBlacklist> long count(Example<S> example) {
        throw unsupported("Unimplemented method 'count'");
    }

    @Override
    public <S extends JwtBlacklist> boolean exists(Example<S> example) {
        throw unsupported("Unimplemented method 'exists'");
    }

    @Override
    public <S extends JwtBlacklist, R> R findBy(Example<S> example,
            Function<FetchableFluentQuery<S>, R> queryFunction) {
        throw unsupported("Unimplemented method 'findBy'");
    }

    @Override
    public boolean existsByJti(String jti) {
        throw unsupported("Unimplemented method 'existsByJti'");
    }
}