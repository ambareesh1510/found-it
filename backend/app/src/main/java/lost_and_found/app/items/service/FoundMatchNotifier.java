package lost_and_found.app.items.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lost_and_found.app.items.domain.ItemPost;
import lost_and_found.app.items.infra.ItemPostRepository;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.infra.UserSettingsRepository;
import lost_and_found.app.shared.service.EmailService;

/**
 * Emails the owners of open LOST reports that a newly posted FOUND item may match (the Settings
 * "Found Item Matches" alert). Runs asynchronously so posting a found item never blocks on or
 * fails because of email. The email carries no found-item detail — see {@link EmailService}.
 */
@Service
public class FoundMatchNotifier {

    private static final Logger log = LoggerFactory.getLogger(FoundMatchNotifier.class);

    /** 1 mile, the lost-search default radius. */
    static final double MATCH_RADIUS_METERS = 1609.34;

    private final ItemPostRepository itemPostRepository;
    private final UserSettingsRepository settingsRepository;
    private final EmailService emailService;

    public FoundMatchNotifier(
        ItemPostRepository itemPostRepository,
        UserSettingsRepository settingsRepository,
        EmailService emailService
    ) {
        this.itemPostRepository = itemPostRepository;
        this.settingsRepository = settingsRepository;
        this.emailService = emailService;
    }

    /**
     * Emails each eligible owner of an open LOST report matching a just-posted FOUND item. Takes the
     * found item's fields directly (not its id) so it does not race the still-committing create
     * transaction. One email per owner; a single failed send does not stop the others.
     *
     * @param category the found item's category
     * @param location the found item's point
     * @param finderUserId the poster, excluded from matches
     */
    @Async
    @Transactional(readOnly = true)
    public void notifyForFoundItem(String category, Point location, Long finderUserId) {
        if (category == null || location == null || finderUserId == null) return;

        List<ItemPost> matches = itemPostRepository.findMatchingLostReports(
            category, location, MATCH_RADIUS_METERS, finderUserId);
        log.debug("found-match check: category={}, finder={}, {} candidate report(s)",
            category, finderUserId, matches.size());

        Set<Long> emailed = new HashSet<>();
        for (ItemPost lostReport : matches) {
            User owner = lostReport.getUser();
            if (owner == null || owner.getId() == null) continue;
            if (owner.getId().equals(finderUserId)) continue;   // defensive; SQL already excludes
            if (!emailed.add(owner.getId())) continue;          // one email per owner
            if (!isEligible(owner.getId())) continue;

            try {
                emailService.sendFoundMatchEmail(owner.getEmail(), lostReport.getTitle());
            } catch (Exception e) {
                // Best-effort: log and keep notifying the rest.
                log.warn("Found-match email to user {} failed: {}", owner.getId(), e.getMessage());
            }
        }
    }

    /** Eligible when both the event toggle and the email-channel toggle are on. Missing row = not eligible. */
    private boolean isEligible(Long userId) {
        return settingsRepository.findById(userId)
            .map(s -> s.isFoundItemMatches() && s.isEmailUpdates())
            .orElse(false);
    }
}
