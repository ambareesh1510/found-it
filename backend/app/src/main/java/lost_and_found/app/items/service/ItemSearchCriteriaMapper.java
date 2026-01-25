package lost_and_found.app.items.service;

import org.springframework.stereotype.Service;

import lost_and_found.app.items.domain.ItemPostStatus;
import lost_and_found.app.items.domain.PostType;

@Service
public class ItemSearchCriteriaMapper {

    public ItemSearchCriteria fromQueryParams(
        String status,
        String category,
        Long userId
    ) {
        return fromQueryParams(status, category, userId, null);
    }

    public ItemSearchCriteria fromQueryParams(
        String status,
        String category,
        Long userId,
        String postType
    ) {
        return new ItemSearchCriteria(
            parseStatus(status),
            normalizeText(category),
            userId,
            parsePostType(postType)
        );
    }

    private ItemPostStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return ItemPostStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("status must be one of: OPEN, RESOLVED.");
        }
    }

    private PostType parsePostType(String postType) {
        if (postType == null || postType.isBlank()) {
            return null;
        }

        try {
            return PostType.valueOf(postType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("postType must be one of: LOST, FOUND.");
        }
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
