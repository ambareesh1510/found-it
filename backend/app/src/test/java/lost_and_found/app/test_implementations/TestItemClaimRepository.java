package lost_and_found.app.test_implementations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.data.domain.Example;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;

import lost_and_found.app.claims.domain.ClaimStatus;
import lost_and_found.app.claims.domain.ItemClaim;
import lost_and_found.app.claims.infra.ItemClaimRepository;

public class TestItemClaimRepository implements ItemClaimRepository {

    private final Map<Long, ItemClaim> store = new HashMap<>();
    private long nextId = 1L;

    @Override
    public List<ItemClaim> findByItemPostId(Long itemPostId) {
        return store.values().stream()
            .filter(claim -> claim.getItemPost() != null && itemPostId.equals(claim.getItemPost().getId()))
            .toList();
    }

    @Override
    public List<ItemClaim> findByClaimantUserId(Long claimantUserId) {
        return store.values().stream()
            .filter(claim -> claim.getClaimantUser() != null
                && claimantUserId.equals(claim.getClaimantUser().getId()))
            .toList();
    }

    @Override
    public List<ItemClaim> findByStatus(ClaimStatus status) {
        return store.values().stream()
            .filter(claim -> claim.getStatus() == status)
            .toList();
    }

    @Override
    public Optional<ItemClaim> findByItemPostIdAndClaimantUserId(Long itemPostId, Long claimantUserId) {
        return store.values().stream()
            .filter(claim -> claim.getItemPost() != null && itemPostId.equals(claim.getItemPost().getId()))
            .filter(claim -> claim.getClaimantUser() != null
                && claimantUserId.equals(claim.getClaimantUser().getId()))
            .findFirst();
    }

    @Override
    public boolean existsByItemPostIdAndClaimantUserId(Long itemPostId, Long claimantUserId) {
        return findByItemPostIdAndClaimantUserId(itemPostId, claimantUserId).isPresent();
    }

    @Override
    public <S extends ItemClaim> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(nextId++);
        }
        store.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public <S extends ItemClaim> List<S> saveAll(Iterable<S> entities) {
        List<S> saved = new ArrayList<>();
        for (S entity : entities) {
            saved.add(save(entity));
        }
        return saved;
    }

    @Override
    public Optional<ItemClaim> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public boolean existsById(Long id) {
        return store.containsKey(id);
    }

    @Override
    public List<ItemClaim> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<ItemClaim> findAllById(Iterable<Long> ids) {
        List<ItemClaim> result = new ArrayList<>();
        for (Long id : ids) {
            ItemClaim claim = store.get(id);
            if (claim != null) {
                result.add(claim);
            }
        }
        return result;
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }

    @Override
    public void delete(ItemClaim entity) {
        if (entity != null && entity.getId() != null) {
            store.remove(entity.getId());
        }
    }

    @Override
    public void deleteAllById(Iterable<? extends Long> ids) {
        for (Long id : ids) {
            store.remove(id);
        }
    }

    @Override
    public void deleteAll(Iterable<? extends ItemClaim> entities) {
        for (ItemClaim entity : entities) {
            delete(entity);
        }
    }

    @Override
    public void deleteAll() {
        store.clear();
    }

    // Unsupported JpaRepository/PagingAndSortingRepository methods

    @Override
    public void flush() {
    }

    @Override
    public <S extends ItemClaim> S saveAndFlush(S entity) {
        return save(entity);
    }

    @Override
    public <S extends ItemClaim> List<S> saveAllAndFlush(Iterable<S> entities) {
        return saveAll(entities);
    }

    @Override
    public void deleteAllInBatch(Iterable<ItemClaim> entities) {
        deleteAll(entities);
    }

    @Override
    public void deleteAllByIdInBatch(Iterable<Long> ids) {
        deleteAllById(ids);
    }

    @Override
    public void deleteAllInBatch() {
        deleteAll();
    }

    @Override
    public ItemClaim getOne(Long id) {
        return findById(id).orElseThrow();
    }

    @Override
    public ItemClaim getById(Long id) {
        return findById(id).orElseThrow();
    }

    @Override
    public ItemClaim getReferenceById(Long id) {
        return findById(id).orElseThrow();
    }

    @Override
    public <S extends ItemClaim> Optional<S> findOne(org.springframework.data.domain.Example<S> example) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends ItemClaim> List<S> findAll(org.springframework.data.domain.Example<S> example) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends ItemClaim> List<S> findAll(
        org.springframework.data.domain.Example<S> example,
        org.springframework.data.domain.Sort sort
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends ItemClaim> org.springframework.data.domain.Page<S> findAll(
        org.springframework.data.domain.Example<S> example,
        org.springframework.data.domain.Pageable pageable
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends ItemClaim> long count(org.springframework.data.domain.Example<S> example) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends ItemClaim> boolean exists(org.springframework.data.domain.Example<S> example) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ItemClaim> findAll(org.springframework.data.domain.Sort sort) {
        throw new UnsupportedOperationException();
    }

    @Override
    public org.springframework.data.domain.Page<ItemClaim> findAll(
        org.springframework.data.domain.Pageable pageable
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends ItemClaim, R> R findBy(Example<S> example, Function<FetchableFluentQuery<S>, R> queryFunction) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findBy'");
    }
}