package lost_and_found.app.shared.dto;

import lost_and_found.app.shared.domain.UserSettings;

/**
 * The Settings-screen toggles, as returned to and accepted from the client.
 * Primitive booleans, so a PUT must send every field; the client always does.
 */
public record UserSettingsDTO(
    boolean locationAccess,
    boolean privateProfile,
    boolean pushNotifications,
    boolean lostItemAlerts,
    boolean foundItemMatches,
    boolean notificationSound,
    boolean emailUpdates
) {

    public static UserSettingsDTO from(UserSettings s) {
        return new UserSettingsDTO(
            s.isLocationAccess(),
            s.isPrivateProfile(),
            s.isPushNotifications(),
            s.isLostItemAlerts(),
            s.isFoundItemMatches(),
            s.isNotificationSound(),
            s.isEmailUpdates()
        );
    }

    /** Copies these values onto an existing settings row. */
    public void applyTo(UserSettings s) {
        s.setLocationAccess(locationAccess);
        s.setPrivateProfile(privateProfile);
        s.setPushNotifications(pushNotifications);
        s.setLostItemAlerts(lostItemAlerts);
        s.setFoundItemMatches(foundItemMatches);
        s.setNotificationSound(notificationSound);
        s.setEmailUpdates(emailUpdates);
    }
}
