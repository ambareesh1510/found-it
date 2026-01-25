package lost_and_found.app;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import lost_and_found.app.items.domain.ItemPost;
import lost_and_found.app.items.domain.ItemPostTag;
import lost_and_found.app.items.domain.ItemPostTagId;
import lost_and_found.app.items.domain.Tag;
import lost_and_found.app.items.infra.ItemPostTagRepository;
import lost_and_found.app.items.infra.TagRepository;
import lost_and_found.app.items.service.PointUtil;
import lost_and_found.app.items.service.VisionTaggingService;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.test_implementations.TestItemPostRepository;

class VisionTaggingServiceTest {

    private TestItemPostRepository itemPostRepository;
    private InMemoryTagRepository tagRepository;
    private InMemoryItemPostTagRepository itemPostTagRepository;
    private PointUtil pointUtil;

    @BeforeEach
    void setUp() {
        itemPostRepository = new TestItemPostRepository();
        tagRepository = new InMemoryTagRepository();
        itemPostTagRepository = new InMemoryItemPostTagRepository();
        pointUtil = new PointUtil();
    }

    @Test
    void tagItemAsync_shouldNoOpWhenDisabled() {
        VisionTaggingService service = buildService(false, "some-token");

        ItemPost item = saveItem();
        service.tagItemAsync(item.getId());

        assertTrue(itemPostTagRepository.findByItemPostId(item.getId()).isEmpty());
    }

    @Test
    void tagItemAsync_shouldNoOpWhenTokenBlank() {
        VisionTaggingService service = buildService(true, "");

        ItemPost item = saveItem();
        service.tagItemAsync(item.getId());

        assertTrue(itemPostTagRepository.findByItemPostId(item.getId()).isEmpty());
    }

    @Test
    void tagItemAsync_shouldNoOpWhenItemNotFound() {
        VisionTaggingService service = buildService(true, "test-token");

        // Should not throw - fire and forget
        assertDoesNotThrow(() -> service.tagItemAsync(999L));
    }

    @Test
    void tagItemAsync_shouldNoOpWhenImageNotBase64() {
        VisionTaggingService service = buildService(true, "test-token");

        ItemPost item = saveItem();
        item.setImageUrl("https://example.com/image.jpg");
        itemPostRepository.save(item);

        service.tagItemAsync(item.getId());

        assertTrue(itemPostTagRepository.findByItemPostId(item.getId()).isEmpty());
    }

    @Test
    void tagItemAsync_shouldNotCrashOnApiError() {
        // Uses a bad URL so the HTTP call fails - verifies fire-and-forget
        VisionTaggingService service = new VisionTaggingService(
                "http://localhost:19999/", "nonexistent-model", "test-token", true,
                itemPostRepository, tagRepository, itemPostTagRepository
        );

        ItemPost item = saveItem();
        // Small valid base64 PNG (1x1 pixel)
        item.setImageUrl("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
        itemPostRepository.save(item);

        assertDoesNotThrow(() -> service.tagItemAsync(item.getId()));
    }

    @Test
    void detectDominantColor_shouldReturnBlueForBlueImage() throws Exception {
        VisionTaggingService service = buildService(true, "test-token");

        // Create a 10x10 solid blue image
        BufferedImage blueImg = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 10; y++)
            for (int x = 0; x < 10; x++)
                blueImg.setRGB(x, y, 0x0055FF);
        byte[] blueBytes = toBytes(blueImg);

        String color = service.detectDominantColor(blueBytes);
        assertEquals("blue", color);
    }

    @Test
    void detectDominantColor_shouldReturnRedForRedImage() throws Exception {
        VisionTaggingService service = buildService(true, "test-token");

        BufferedImage redImg = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 10; y++)
            for (int x = 0; x < 10; x++)
                redImg.setRGB(x, y, 0xFF1010);
        byte[] redBytes = toBytes(redImg);

        String color = service.detectDominantColor(redBytes);
        assertEquals("red", color);
    }

    @Test
    void detectDominantColor_shouldReturnBlackForDarkImage() throws Exception {
        VisionTaggingService service = buildService(true, "test-token");

        BufferedImage darkImg = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 10; y++)
            for (int x = 0; x < 10; x++)
                darkImg.setRGB(x, y, 0x0A0A0A);
        byte[] darkBytes = toBytes(darkImg);

        String color = service.detectDominantColor(darkBytes);
        assertEquals("black", color);
    }

    private byte[] toBytes(BufferedImage img) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private VisionTaggingService buildService(boolean enabled, String apiToken) {
        return new VisionTaggingService(
                "http://localhost", "test-model", apiToken, enabled,
                itemPostRepository, tagRepository, itemPostTagRepository
        );
    }

    private ItemPost saveItem() {
        User user = User.builder()
                .username("test")
                .email("test@test.com")
                .password("pw")
                .enabled(true)
                .build();

        ItemPost item = new ItemPost();
        item.setUser(user);
        item.setTitle("Lost Phone");
        item.setDescription("black phone");
        item.setCategory("Electronics");
        item.setImageUrl("img");
        item.setLocation(pointUtil.create(-84.39, 33.77));
        return itemPostRepository.save(item);
    }

    // ---- In-memory test doubles ----

    static class InMemoryTagRepository implements TagRepository {
        private final Map<Long, Tag> store = new HashMap<>();
        private long nextId = 1L;

        @Override
        public Optional<Tag> findByNameIgnoreCase(String name) {
            return store.values().stream()
                    .filter(t -> t.getName().equalsIgnoreCase(name))
                    .findFirst();
        }

        @Override public List<Tag> findByIsActiveTrue() { return List.of(); }
        @Override public List<Tag> findByCategoryIgnoreCase(String category) { return List.of(); }

        @Override
        public <S extends Tag> S save(S entity) {
            if (entity.getId() == null) entity.setId(nextId++);
            store.put(entity.getId(), entity);
            return entity;
        }

        @Override public <S extends Tag> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public Optional<Tag> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public boolean existsById(Long id) { return store.containsKey(id); }
        @Override public List<Tag> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<Tag> findAllById(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(Long id) { store.remove(id); }
        @Override public void delete(Tag entity) { store.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> ids) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends Tag> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { store.clear(); }
        @Override public void flush() {}
        @Override public <S extends Tag> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Tag> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<Tag> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch() { throw new UnsupportedOperationException(); }
        @Override public Tag getOne(Long id) { return findById(id).orElseThrow(); }
        @Override public Tag getById(Long id) { return findById(id).orElseThrow(); }
        @Override public Tag getReferenceById(Long id) { return findById(id).orElseThrow(); }
        @Override public <S extends Tag> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Tag> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Tag> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends Tag> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends Tag> long count(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends Tag> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public List<Tag> findAll(Sort sort) { throw new UnsupportedOperationException(); }
        @Override public Page<Tag> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends Tag, R> R findBy(Example<S> example, Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
    }

    static class InMemoryItemPostTagRepository implements ItemPostTagRepository {
        private final List<ItemPostTag> store = new ArrayList<>();

        @Override
        public List<ItemPostTag> findByItemPostId(Long itemPostId) {
            return store.stream().filter(t -> t.getId().getItemPostId().equals(itemPostId)).toList();
        }

        @Override
        public List<ItemPostTag> findByTagId(Long tagId) {
            return store.stream().filter(t -> t.getId().getTagId().equals(tagId)).toList();
        }

        @Override
        public boolean existsByItemPostIdAndTagId(Long itemPostId, Long tagId) {
            return store.stream().anyMatch(t ->
                    t.getId().getItemPostId().equals(itemPostId) && t.getId().getTagId().equals(tagId));
        }

        @Override
        public void deleteByItemPostIdAndTagId(Long itemPostId, Long tagId) {
            store.removeIf(t -> t.getId().getItemPostId().equals(itemPostId) && t.getId().getTagId().equals(tagId));
        }

        @Override
        public <S extends ItemPostTag> S save(S entity) {
            store.add(entity);
            return entity;
        }

        @Override public <S extends ItemPostTag> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public Optional<ItemPostTag> findById(ItemPostTagId id) { return store.stream().filter(t -> t.getId().equals(id)).findFirst(); }
        @Override public boolean existsById(ItemPostTagId id) { return findById(id).isPresent(); }
        @Override public List<ItemPostTag> findAll() { return new ArrayList<>(store); }
        @Override public List<ItemPostTag> findAllById(Iterable<ItemPostTagId> ids) { throw new UnsupportedOperationException(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(ItemPostTagId id) { store.removeIf(t -> t.getId().equals(id)); }
        @Override public void delete(ItemPostTag entity) { store.remove(entity); }
        @Override public void deleteAllById(Iterable<? extends ItemPostTagId> ids) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends ItemPostTag> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { store.clear(); }
        @Override public void flush() {}
        @Override public <S extends ItemPostTag> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends ItemPostTag> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<ItemPostTag> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllByIdInBatch(Iterable<ItemPostTagId> ids) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch() { throw new UnsupportedOperationException(); }
        @Override public ItemPostTag getOne(ItemPostTagId id) { return findById(id).orElseThrow(); }
        @Override public ItemPostTag getById(ItemPostTagId id) { return findById(id).orElseThrow(); }
        @Override public ItemPostTag getReferenceById(ItemPostTagId id) { return findById(id).orElseThrow(); }
        @Override public <S extends ItemPostTag> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends ItemPostTag> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends ItemPostTag> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends ItemPostTag> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends ItemPostTag> long count(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends ItemPostTag> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public List<ItemPostTag> findAll(Sort sort) { throw new UnsupportedOperationException(); }
        @Override public Page<ItemPostTag> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends ItemPostTag, R> R findBy(Example<S> example, Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
    }
}
