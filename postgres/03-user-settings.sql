-- Per-user preference toggles (docs/design/ui-spec.md, screen 09-settings).
-- One row per user, created lazily by the app on first read. Defaults match the prototype.
CREATE TABLE user_settings (
    user_id            BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    location_access    BOOLEAN NOT NULL DEFAULT TRUE,
    private_profile    BOOLEAN NOT NULL DEFAULT FALSE,
    push_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    lost_item_alerts   BOOLEAN NOT NULL DEFAULT TRUE,
    found_item_matches BOOLEAN NOT NULL DEFAULT TRUE,
    notification_sound BOOLEAN NOT NULL DEFAULT TRUE,
    email_updates      BOOLEAN NOT NULL DEFAULT FALSE
);
