package lost_and_found.app.test_implementations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.DeleteSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.UpdateSpecification;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;

import lost_and_found.app.items.domain.ItemPost;
import lost_and_found.app.items.domain.ItemPostStatus;
import lost_and_found.app.items.infra.ItemPostRepository;
import lost_and_found.app.items.service.ItemSpecifications;

public class TestItemPostRepository implements ItemPostRepository {

    private final Map<Long, ItemPost> store = new HashMap<>();
    private long nextId = 1L;

    @Override
    public List<ItemPost> findByUserId(Long userId) {
        return store.values().stream()
            .filter(item -> item.getUser() != null && userId.equals(item.getUser().getId()))
            .toList();
    }

    @Override
    public List<ItemPost> findByStatus(ItemPostStatus status) {
        return store.values().stream()
            .filter(item -> item.getStatus() == status)
            .toList();
    }

    @Override
    public List<ItemPost> findByCategoryIgnoreCase(String category) {
        return store.values().stream()
            .filter(item -> item.getCategory() != null && item.getCategory().equalsIgnoreCase(category))
            .toList();
    }


    @Override
    public List<ItemPost> findNearby(Point center, double radiusMeters) {
        List<ItemPost> nearby = new ArrayList<>();

        for (ItemPost item : store.values()) {
            Point p = item.getLocation();
            if (p == null) continue;

            double meters = distanceMeters(center, p);
            if (meters <= radiusMeters) {
                nearby.add(item);
            }
        }

        nearby.sort(Comparator.comparingDouble(item -> distanceMeters(center, item.getLocation())));
        return nearby;
    }

    private double distanceMeters(Point a, Point b) {
        // crude approximation good enough for unit tests
        double lat1 = a.getY();
        double lon1 = a.getX();
        double lat2 = b.getY();
        double lon2 = b.getX();

        double latMeters = (lat2 - lat1) * 111_320.0;
        double lonMeters = (lon2 - lon1) * 111_320.0 * Math.cos(Math.toRadians((lat1 + lat2) / 2.0));

        return Math.sqrt(latMeters * latMeters + lonMeters * lonMeters);
    }

    @Override
    public <S extends ItemPost> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(nextId++);
        }
        store.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public <S extends ItemPost> List<S> saveAll(Iterable<S> entities) {
        List<S> saved = new ArrayList<>();
        for (S entity : entities) {
            saved.add(save(entity));
        }
        return saved;
    }

    @Override
    public Optional<ItemPost> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public boolean existsById(Long id) {
        return store.containsKey(id);
    }

    @Override
    public List<ItemPost> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<ItemPost> findAll(Specification<ItemPost> spec) {
        if (spec == null) {
            return findAll();
        }

        if (spec instanceof ItemSpecifications.ItemSearchSpecification itemSpec) {
            return store.values().stream()
                .filter(itemSpec::matches)
                .toList();
        }

        throw new UnsupportedOperationException("Unsupported specification type: " + spec.getClass().getName());
    }

    @Override
    public List<ItemPost> findAllById(Iterable<Long> ids) {
        List<ItemPost> result = new ArrayList<>();
        for (Long id : ids) {
            ItemPost item = store.get(id);
            if (item != null) {
                result.add(item);
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
    public void delete(ItemPost entity) {
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
    public void deleteAll(Iterable<? extends ItemPost> entities) {
        for (ItemPost entity : entities) {
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
    public <S extends ItemPost> S saveAndFlush(S entity) {
        return save(entity);
    }

    @Override
    public <S extends ItemPost> List<S> saveAllAndFlush(Iterable<S> entities) {
        return saveAll(entities);
    }

    @Override
    public void deleteAllInBatch(Iterable<ItemPost> entities) {
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
    public ItemPost getOne(Long id) {
        return findById(id).orElseThrow();
    }

    @Override
    public ItemPost getById(Long id) {
        return findById(id).orElseThrow();
    }

    @Override
    public ItemPost getReferenceById(Long id) {
        return findById(id).orElseThrow();
    }

    @Override
    public <S extends ItemPost> Optional<S> findOne(org.springframework.data.domain.Example<S> example) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends ItemPost> List<S> findAll(org.springframework.data.domain.Example<S> example) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends ItemPost> List<S> findAll(
        org.springframework.data.domain.Example<S> example,
        org.springframework.data.domain.Sort sort
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends ItemPost> Page<S> findAll(
        org.springframework.data.domain.Example<S> example,
        org.springframework.data.domain.Pageable pageable
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends ItemPost> long count(org.springframework.data.domain.Example<S> example) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends ItemPost> boolean exists(org.springframework.data.domain.Example<S> example) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ItemPost> findAll(org.springframework.data.domain.Sort sort) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<ItemPost> findOne(Specification<ItemPost> spec) {
        List<ItemPost> results = findAll(spec);
        if (results.isEmpty()) {
            return Optional.empty();
        }
        if (results.size() > 1) {
            throw new UnsupportedOperationException("findOne with multiple matches is not supported in this test repository.");
        }
        return Optional.of(results.getFirst());
    }

    @Override
    public Page<ItemPost> findAll(Specification<ItemPost> spec, org.springframework.data.domain.Pageable pageable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Page<ItemPost> findAll(
        Specification<ItemPost> spec,
        Specification<ItemPost> countSpec,
        org.springframework.data.domain.Pageable pageable
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ItemPost> findAll(Specification<ItemPost> spec, org.springframework.data.domain.Sort sort) {
        return findAll(spec);
    }

    @Override
    public long count(Specification<ItemPost> spec) {
        return findAll(spec).size();
    }

    @Override
    public boolean exists(Specification<ItemPost> spec) {
        return !findAll(spec).isEmpty();
    }

    @Override
    public long update(UpdateSpecification<ItemPost> updateSpecification) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long delete(DeleteSpecification<ItemPost> deleteSpecification) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends ItemPost, R> R findBy(
        Specification<ItemPost> spec,
        Function<? super org.springframework.data.jpa.repository.JpaSpecificationExecutor.SpecificationFluentQuery<S>, R> queryFunction
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public org.springframework.data.domain.Page<ItemPost> findAll(
        org.springframework.data.domain.Pageable pageable
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends ItemPost, R> R findBy(Example<S> example, Function<FetchableFluentQuery<S>, R> queryFunction) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findBy'");
    }
}
