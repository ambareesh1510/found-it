package lost_and_found.app.searches.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lost_and_found.app.exceptions.UserNotFoundException;
import lost_and_found.app.items.domain.ItemPost;
import lost_and_found.app.items.domain.ItemPostStatus;
import lost_and_found.app.items.domain.PostType;
import lost_and_found.app.items.infra.ItemPostRepository;
import lost_and_found.app.items.service.PointUtil;
import lost_and_found.app.searches.domain.LostSearch;
import lost_and_found.app.searches.dto.request.LostSearchRequestDTO;
import lost_and_found.app.searches.dto.request.TimelinePointDTO;
import lost_and_found.app.searches.dto.response.FoundItemSummaryDTO;
import lost_and_found.app.searches.dto.response.LostSearchResultDTO;
import lost_and_found.app.searches.infra.LostSearchRepository;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.infra.UserRepository;

/**
 * Records a loser's declared search and returns the FOUND posts that match it.
 * Matching is server-side only: the client never receives the full found list.
 */
@Service
public class LostSearchService {

    static final double TIMELINE_RADIUS_METERS = 804.672;
    static final Duration TIMELINE_WINDOW = Duration.ofHours(24);

    private final UserRepository userRepository;
    private final LostSearchRepository lostSearchRepository;
    private final ItemPostRepository itemPostRepository;
    private final PointUtil pointUtil;
    private final Clock clock;

    @Autowired
    public LostSearchService(
        UserRepository userRepository,
        LostSearchRepository lostSearchRepository,
        ItemPostRepository itemPostRepository,
        PointUtil pointUtil
    ) {
        this(userRepository, lostSearchRepository, itemPostRepository, pointUtil, Clock.systemUTC());
    }

    LostSearchService(
        UserRepository userRepository,
        LostSearchRepository lostSearchRepository,
        ItemPostRepository itemPostRepository,
        PointUtil pointUtil,
        Clock clock
    ) {
        this.userRepository = userRepository;
        this.lostSearchRepository = lostSearchRepository;
        this.itemPostRepository = itemPostRepository;
        this.pointUtil = pointUtil;
        this.clock = clock;
    }

    @Transactional
    public LostSearchResultDTO searchFoundItems(Long userId, LostSearchRequestDTO req) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User " + userId + " not found"));

        if (req.useTimeline()) {
            return searchTimeline(user, req);
        }

        Point center = pointUtil.create(req.longitude(), req.latitude());

        // Persist the declaration first so the record exists even if matching fails.
        LostSearch search = new LostSearch();
        search.setUser(user);
        search.setCategory(req.category().trim());
        search.setLocation(center);
        search.setSearchRadiusMeters(req.radiusMeters());
        search.setWindowStart(req.windowStart());
        search.setWindowEnd(req.windowEnd());
        search.setUsedTimeline(false);
        LostSearch saved = lostSearchRepository.save(search);

        // PostGIS does the radius check; category, type, status, and window are applied here.
        List<FoundItemSummaryDTO> results = itemPostRepository.findNearby(center, req.radiusMeters())
            .stream()
            .filter(item -> matches(item, req))
            .map(this::toSummary)
            .toList();

        return new LostSearchResultDTO(saved.getId(), results);
    }

    private LostSearchResultDTO searchTimeline(User user, LostSearchRequestDTO req) {
        Instant windowEnd = clock.instant();
        Instant windowStart = windowEnd.minus(TIMELINE_WINDOW);
        List<TimelinePointDTO> points = req.timelinePoints().stream()
            .filter(point -> !point.recordedAt().isBefore(windowStart) && !point.recordedAt().isAfter(windowEnd))
            .toList();
        if (points.isEmpty()) {
            throw new IllegalArgumentException("Timeline has no location points from the previous 24 hours.");
        }

        LostSearch search = new LostSearch();
        search.setUser(user);
        search.setCategory(req.category().trim());
        search.setLocation(null);
        search.setSearchRadiusMeters(TIMELINE_RADIUS_METERS);
        search.setWindowStart(windowStart);
        search.setWindowEnd(windowEnd);
        search.setUsedTimeline(true);
        LostSearch saved = lostSearchRepository.save(search);

        Map<Long, ItemPost> matches = new LinkedHashMap<>();
        for (TimelinePointDTO point : points) {
            Point center = pointUtil.create(point.longitude(), point.latitude());
            itemPostRepository.findNearby(center, TIMELINE_RADIUS_METERS).stream()
                .filter(item -> matches(item, req.category(), windowStart, windowEnd))
                .forEach(item -> matches.putIfAbsent(item.getId(), item));
        }

        return new LostSearchResultDTO(saved.getId(), matches.values().stream().map(this::toSummary).toList());
    }

    private boolean matches(ItemPost item, LostSearchRequestDTO req) {
        return matches(item, req.category(), req.windowStart(), req.windowEnd());
    }

    private boolean matches(ItemPost item, String category, Instant windowStart, Instant windowEnd) {
        return item.getPostType() == PostType.FOUND
            && item.getStatus() == ItemPostStatus.OPEN
            && item.getCategory() != null
            && item.getCategory().equalsIgnoreCase(category.trim())
            && item.getEventTime() != null
            && !item.getEventTime().isBefore(windowStart)
            && !item.getEventTime().isAfter(windowEnd);
    }

    private FoundItemSummaryDTO toSummary(ItemPost item) {
        return new FoundItemSummaryDTO(
            item.getId(),
            item.getTitle(),
            item.getDescription(),
            item.getCategory(),
            item.getImageUrl(),
            item.getLocationText(),
            item.getEventTime()
        );
    }
}
