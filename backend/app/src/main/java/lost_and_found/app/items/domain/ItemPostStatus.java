package lost_and_found.app.items.domain;

public enum ItemPostStatus {
    OPEN,
    /** The poster has proposed a resolution with a chosen counterparty; waiting for that person to confirm. */
    PENDING_RESOLUTION,
    RESOLVED
}
