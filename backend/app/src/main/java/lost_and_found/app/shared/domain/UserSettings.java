package lost_and_found.app.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Per-user preference toggles shown on the Settings screen (docs/design/ui-spec.md).
 * Shares its primary key with {@link User} via {@link MapsId}, so there is exactly one
 * settings row per user. Defaults match the prototype's intended defaults.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    // --- Privacy & Location ---

    @Column(name = "location_access", nullable = false)
    @Builder.Default
    private boolean locationAccess = true;

    @Column(name = "private_profile", nullable = false)
    @Builder.Default
    private boolean privateProfile = false;

    // --- Notifications ---

    @Column(name = "push_notifications", nullable = false)
    @Builder.Default
    private boolean pushNotifications = true;

    @Column(name = "lost_item_alerts", nullable = false)
    @Builder.Default
    private boolean lostItemAlerts = true;

    @Column(name = "found_item_matches", nullable = false)
    @Builder.Default
    private boolean foundItemMatches = true;

    @Column(name = "notification_sound", nullable = false)
    @Builder.Default
    private boolean notificationSound = true;

    @Column(name = "email_updates", nullable = false)
    @Builder.Default
    private boolean emailUpdates = false;

    /**
     * A fresh settings row with the prototype defaults, linked to {@code user}. The id is left
     * null on purpose: {@code @MapsId} derives it from {@code user} when the row is persisted, and
     * setting it here would make Hibernate treat the new row as detached and fail to merge it.
     */
    public static UserSettings defaultsFor(User user) {
        return UserSettings.builder().user(user).build();
    }
}
