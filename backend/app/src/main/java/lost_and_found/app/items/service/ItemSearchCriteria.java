package lost_and_found.app.items.service;

import lost_and_found.app.items.domain.ItemPostStatus;
import lost_and_found.app.items.domain.PostType;

public record ItemSearchCriteria(
    ItemPostStatus status,
    String category,
    Long userId,
    PostType postType
) {

    /** Keeps the pre-postType call sites (and tests) compiling. */
    public ItemSearchCriteria(ItemPostStatus status, String category, Long userId) {
        this(status, category, userId, null);
    }
}
