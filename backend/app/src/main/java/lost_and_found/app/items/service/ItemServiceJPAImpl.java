package lost_and_found.app.items.service;

import java.util.List;

import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lost_and_found.app.exceptions.ItemNotFoundException;
import lost_and_found.app.exceptions.UserMismatchException;
import lost_and_found.app.exceptions.UserNotFoundException;
import lost_and_found.app.items.domain.ItemPost;
import lost_and_found.app.items.domain.ItemPostStatus;
import lost_and_found.app.items.domain.PostType;
import lost_and_found.app.items.dto.request.CreateItemDTO;
import lost_and_found.app.items.dto.request.UpdateItemDTO;
import lost_and_found.app.items.dto.response.ItemPostResponseDTO;
import lost_and_found.app.items.infra.ItemPostRepository;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.infra.UserRepository;

/**
 * A class that handles JPA interactions and business logic for items.
 */
@Service
public class ItemServiceJPAImpl implements ItemService {

    private final UserRepository userRepository;
    private final ItemPostRepository itemPostRepository;
    private final PointUtil pointUtil;
    private final ItemMapper itemMapper;

    public ItemServiceJPAImpl(
        UserRepository userRepository,
        ItemPostRepository itemPostRepository,
        PointUtil pointUtil,
        ItemMapper itemMapper
    ) {
        this.userRepository = userRepository;
        this.itemPostRepository = itemPostRepository;
        this.pointUtil = pointUtil;
        this.itemMapper = itemMapper;
    }

    @Transactional
    @Override
    public ItemPostResponseDTO createItem(Long userId, CreateItemDTO req) throws UserNotFoundException {
        User user = userRepository.findById(userId).orElseThrow(
            () -> new UserNotFoundException("User " + userId + " not found")
        );

        Point point = pointUtil.create(req.longitude(), req.latitude());

        ItemPost item = new ItemPost();
        item.setUser(user);
        item.setTitle(req.title());
        item.setDescription(req.description());
        item.setCategory(req.category());
        item.setStatus(ItemPostStatus.OPEN);
        item.setPostType(req.postType() == null ? PostType.LOST : req.postType());
        item.setImageUrl(blankToNull(req.imageUrl()));
        item.setImageUrl2(req.imageUrl2());
        item.setLocation(point);
        item.setLocationText(req.locationText());
        item.setDetailLocation(req.detailLocation());
        item.setEventTime(req.eventTime());
        // The DTO already rejects a reward on FOUND posts; this keeps the entity consistent if called directly.
        item.setRewardCents(item.getPostType() == PostType.LOST ? req.rewardCents() : null);

        ItemPost saved = itemPostRepository.save(item);
        // The poster is the viewer here, so they receive their own detailLocation.
        return itemMapper.toResponse(saved, userId);
    }

    @Transactional
    @Override
    public ItemPostResponseDTO updateItem(Long userId, UpdateItemDTO req) throws ItemNotFoundException, UserMismatchException {
        ItemPost item = itemPostRepository.findById(req.itemId())
            .orElseThrow(() -> new ItemNotFoundException("Item not found"));

        if (Long.compare(userId, item.getUser().getId()) != 0) {
            throw new UserMismatchException("User " + userId + " tried to modify an item that didn't belong to them.");
        }

        if (req.title() != null) item.setTitle(req.title());
        if (req.description() != null) item.setDescription(req.description());
        if (req.category() != null) item.setCategory(req.category());
        if (req.imageUrl() != null) item.setImageUrl(blankToNull(req.imageUrl()));
        if (req.imageUrl2() != null) item.setImageUrl2(req.imageUrl2());
        if (req.locationText() != null) item.setLocationText(req.locationText());
        if (req.detailLocation() != null) item.setDetailLocation(req.detailLocation());
        if (req.eventTime() != null) item.setEventTime(req.eventTime());
        if (req.status() != null) applyStatusChange(item, req.status(), req.resolvedToUserId());

        if (req.latitude() != null && req.longitude() != null) {
            item.setLocation(pointUtil.create(req.longitude(), req.latitude()));
        }

        ItemPost saved = itemPostRepository.save(item);
        return itemMapper.toResponse(saved, userId);
    }

    /**
     * Applies a status change requested by the item's owner.
     * <ul>
     *   <li>RESOLVED with a counterparty: proposes a resolution and moves to PENDING_RESOLUTION; the
     *       counterparty must confirm (see {@link #confirmResolution}) before it becomes RESOLVED.</li>
     *   <li>RESOLVED with no counterparty: the owner closes the item unilaterally (e.g. gave up, no claimant).</li>
     *   <li>OPEN: reopens the item and clears any resolution in progress.</li>
     * </ul>
     */
    private void applyStatusChange(ItemPost item, String requestedStatus, Long resolvedToUserId) {
        ItemPostStatus status = parseStatus(requestedStatus);
        if (status == ItemPostStatus.PENDING_RESOLUTION) {
            // Clients only ever request OPEN or RESOLVED; PENDING_RESOLUTION is an internal transition.
            throw new IllegalArgumentException("status must be one of: OPEN, RESOLVED.");
        }

        if (status == ItemPostStatus.RESOLVED && resolvedToUserId != null) {
            if (Long.compare(resolvedToUserId, item.getUser().getId()) == 0) {
                throw new IllegalArgumentException("You cannot resolve an item with yourself.");
            }
            User counterparty = userRepository.findById(resolvedToUserId)
                .orElseThrow(() -> new UserNotFoundException("User " + resolvedToUserId + " not found"));

            item.setResolvedWithUser(counterparty);
            item.setPosterConfirmedResolution(true);
            item.setCounterpartyConfirmedResolution(false);
            item.setStatus(ItemPostStatus.PENDING_RESOLUTION);
            return;
        }

        if (status == ItemPostStatus.OPEN) {
            clearResolution(item);
        } else if (status == ItemPostStatus.RESOLVED) {
            // Unilateral close with no counterparty to confirm.
            clearResolution(item);
        }
        item.setStatus(status);
    }

    private static void clearResolution(ItemPost item) {
        item.setResolvedWithUser(null);
        item.setPosterConfirmedResolution(false);
        item.setCounterpartyConfirmedResolution(false);
    }

    private static ItemPostStatus parseStatus(String status) {
        try {
            return ItemPostStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("status must be one of: OPEN, RESOLVED.");
        }
    }

    @Transactional
    @Override
    public ItemPostResponseDTO confirmResolution(Long userId, Long itemId)
            throws ItemNotFoundException, UserMismatchException {
        ItemPost item = itemPostRepository.findById(itemId)
            .orElseThrow(() -> new ItemNotFoundException("Item " + itemId + " not found"));

        if (item.getStatus() != ItemPostStatus.PENDING_RESOLUTION || item.getResolvedWithUser() == null) {
            throw new IllegalStateException("This item has no resolution awaiting confirmation.");
        }

        if (Long.compare(userId, item.getResolvedWithUser().getId()) != 0) {
            throw new UserMismatchException("Only the chosen counterparty can confirm this resolution.");
        }

        item.setCounterpartyConfirmedResolution(true);
        if (item.isPosterConfirmedResolution() && item.isCounterpartyConfirmedResolution()) {
            item.setStatus(ItemPostStatus.RESOLVED);
        }

        ItemPost saved = itemPostRepository.save(item);
        return itemMapper.toResponse(saved, userId);
    }

    @Transactional
    @Override
    public void deleteItem(Long userId, Long itemId) throws ItemNotFoundException, UserMismatchException {
        ItemPost item = itemPostRepository.findById(itemId)
            .orElseThrow(() -> new ItemNotFoundException("Item " + itemId + " not found"));
        if (Long.compare(userId, item.getUser().getId()) != 0)
            throw new UserMismatchException("User " + userId + " tried to modify an item that didn't belong to them.");
        itemPostRepository.delete(item);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ItemPostResponseDTO> getNearbyItems(Long viewerId, double lon, double lat, double radiusMeters) {
        Point center = pointUtil.create(lon, lat);

        return itemPostRepository.findNearby(center, radiusMeters)
            .stream()
            .map(item -> itemMapper.toResponse(item, viewerId))
            .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<ItemPostResponseDTO> getItems(Long viewerId, ItemSearchCriteria criteria) {
        Specification<ItemPost> spec = ItemSpecifications.fromCriteria(criteria);

        return itemPostRepository.findAll(spec).stream()
            .map(item -> itemMapper.toResponse(item, viewerId))
            .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public ItemPostResponseDTO getItemById(Long viewerId, Long itemId) throws ItemNotFoundException {
        ItemPost item = itemPostRepository.findById(itemId)
            .orElseThrow(() -> new ItemNotFoundException("Item " + itemId + " not found."));

        return itemMapper.toResponse(item, viewerId);
    }

    /** Stores an empty or whitespace-only photo link as "no photo". */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
