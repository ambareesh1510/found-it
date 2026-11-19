package lost_and_found.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

import lost_and_found.app.items.domain.ItemPost;
import lost_and_found.app.items.domain.ItemPostStatus;
import lost_and_found.app.items.domain.PostType;
import lost_and_found.app.items.service.FoundMatchNotifier;
import lost_and_found.app.items.service.PointUtil;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.domain.UserSettings;
import lost_and_found.app.shared.infra.UserSettingsRepository;
import lost_and_found.app.test_implementations.EmailServiceTest;
import lost_and_found.app.test_implementations.TestItemPostRepository;
import lost_and_found.app.test_implementations.UserSettingsRepositoryTest;

class FoundMatchNotifierTest {

    private static final long FINDER_ID = 999L;

    private TestItemPostRepository items;
    private UserSettingsRepository settings;
    private EmailServiceTest email;
    private FoundMatchNotifier notifier;
    private final PointUtil pointUtil = new PointUtil();
    private final Point center = pointUtil.create(-84.39, 33.77);
    private long nextUserId = 1L;

    @BeforeEach
    void setUp() {
        items = new TestItemPostRepository();
        settings = UserSettingsRepositoryTest.inMemory();
        email = new EmailServiceTest();
        notifier = new FoundMatchNotifier(items, settings, email);
    }

    /** Creates a user with the given alert settings and an open LOST "Keys" report at the center point. */
    private User owner(String name, boolean foundMatches, boolean emailUpdates) {
        User user = User.builder().id(nextUserId++).username(name).email(name + "@test.com").enabled(true).build();
        settings.save(UserSettings.builder()
            .user(user).userId(user.getId())
            .foundItemMatches(foundMatches).emailUpdates(emailUpdates).build());
        saveLostReport(user, "Keys — " + name);
        return user;
    }

    private void saveLostReport(User user, String title) {
        ItemPost post = new ItemPost();
        post.setUser(user);
        post.setTitle(title);
        post.setCategory("Keys");
        post.setPostType(PostType.LOST);
        post.setStatus(ItemPostStatus.OPEN);
        post.setLocation(center);
        items.save(post);
    }

    private void run() {
        notifier.notifyForFoundItem("Keys", center, FINDER_ID);
    }

    @Test
    void emailsAnEligibleOwnerWithTheirReportTitle() {
        User alice = owner("alice", true, true);

        run();

        assertTrue(email.foundMatchRecipients().contains(alice.getEmail()));
        assertEquals("Keys — alice", email.lastFoundMatchTitle(alice.getEmail()));
    }

    @Test
    void skipsWhenFoundItemMatchesOff() {
        User bob = owner("bob", false, true);

        run();

        assertFalse(email.foundMatchRecipients().contains(bob.getEmail()));
    }

    @Test
    void skipsWhenEmailUpdatesOff() {
        User carol = owner("carol", true, false);

        run();

        assertFalse(email.foundMatchRecipients().contains(carol.getEmail()));
    }

    @Test
    void skipsTheFindersOwnMatchingReport() {
        User finder = User.builder().id(FINDER_ID).username("finder").email("finder@test.com").enabled(true).build();
        settings.save(UserSettings.builder().user(finder).userId(FINDER_ID)
            .foundItemMatches(true).emailUpdates(true).build());
        saveLostReport(finder, "Keys — finder");

        run();

        assertFalse(email.foundMatchRecipients().contains("finder@test.com"));
    }

    @Test
    void sendsOneEmailPerOwnerEvenWithMultipleMatchingReports() {
        User dan = owner("dan", true, true);
        saveLostReport(dan, "Keys — dan second report");

        run();

        assertEquals(1, email.foundMatchRecipients().stream().filter(dan.getEmail()::equals).count());
    }

    @Test
    void oneFailedSendDoesNotStopTheRest() {
        User eve = owner("eve", true, true);
        User frank = owner("frank", true, true);
        email.failSendsTo(eve.getEmail());

        run();

        assertFalse(email.foundMatchRecipients().contains(eve.getEmail()));
        assertTrue(email.foundMatchRecipients().contains(frank.getEmail()), "a failure for one recipient must not block others");
    }
}
