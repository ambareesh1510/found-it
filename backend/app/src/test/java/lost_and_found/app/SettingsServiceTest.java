package lost_and_found.app;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.domain.UserSettings;
import lost_and_found.app.shared.dto.UserSettingsDTO;
import lost_and_found.app.shared.infra.UserSettingsRepository;
import lost_and_found.app.shared.service.SettingsService;
import lost_and_found.app.test_implementations.UserRepositoryTest;
import lost_and_found.app.test_implementations.UserSettingsRepositoryTest;

class SettingsServiceTest {

    private UserRepositoryTest users;
    private SettingsService settingsService;
    private User alice;

    @BeforeEach
    void setUp() {
        users = new UserRepositoryTest();
        UserSettingsRepository settingsRepo = UserSettingsRepositoryTest.inMemory();
        settingsService = new SettingsService(settingsRepo, users);
        alice = users.save(User.builder().username("alice").email("alice@test.com").enabled(true).build());
    }

    @Test
    void getOrCreate_returnsPrototypeDefaultsFirstTime() {
        UserSettings settings = settingsService.getOrCreateForUser(alice.getId());

        assertTrue(settings.isLocationAccess());
        assertFalse(settings.isPrivateProfile());
        assertTrue(settings.isPushNotifications());
        assertTrue(settings.isLostItemAlerts());
        assertTrue(settings.isFoundItemMatches());
        assertTrue(settings.isNotificationSound());
        assertFalse(settings.isEmailUpdates());
    }

    @Test
    void update_overwritesTogglesAndPersists() {
        UserSettingsDTO dto = new UserSettingsDTO(false, true, false, false, true, false, true);

        settingsService.update(alice.getId(), dto);
        UserSettings reloaded = settingsService.getOrCreateForUser(alice.getId());

        assertFalse(reloaded.isLocationAccess());
        assertTrue(reloaded.isPrivateProfile());
        assertTrue(reloaded.isEmailUpdates());
    }

    @Test
    void isPrivate_isFalseWhenNoRowExistsAndTrueAfterOptIn() {
        assertFalse(settingsService.isPrivate(alice.getId()), "no settings row yet means not private");

        settingsService.update(alice.getId(), new UserSettingsDTO(true, true, true, true, true, true, false));

        assertTrue(settingsService.isPrivate(alice.getId()));
    }
}
