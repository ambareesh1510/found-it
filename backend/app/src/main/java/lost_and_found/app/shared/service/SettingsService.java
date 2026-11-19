package lost_and_found.app.shared.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lost_and_found.app.exceptions.UserNotFoundException;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.domain.UserSettings;
import lost_and_found.app.shared.dto.UserSettingsDTO;
import lost_and_found.app.shared.infra.UserRepository;
import lost_and_found.app.shared.infra.UserSettingsRepository;

/** Reads and writes the per-user Settings toggles, creating a default row on first use. */
@Service
public class SettingsService {

    private final UserSettingsRepository settingsRepository;
    private final UserRepository userRepository;

    public SettingsService(UserSettingsRepository settingsRepository, UserRepository userRepository) {
        this.settingsRepository = settingsRepository;
        this.userRepository = userRepository;
    }

    /** The user's settings, creating a default row (prototype defaults) the first time. */
    @Transactional
    public UserSettings getOrCreateForUser(Long userId) {
        return settingsRepository.findById(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User " + userId + " not found"));
            return settingsRepository.save(UserSettings.defaultsFor(user));
        });
    }

    /** Overwrites the user's toggles with {@code dto}. */
    @Transactional
    public UserSettings update(Long userId, UserSettingsDTO dto) {
        UserSettings settings = getOrCreateForUser(userId);
        dto.applyTo(settings);
        return settingsRepository.save(settings);
    }

    /**
     * Whether the user has hidden their profile. Read-only and does not create a row, so it is
     * safe to call when merely viewing someone else's profile. Missing row means not private.
     */
    @Transactional(readOnly = true)
    public boolean isPrivate(Long userId) {
        return settingsRepository.findById(userId)
            .map(UserSettings::isPrivateProfile)
            .orElse(false);
    }
}
