package lost_and_found.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import lost_and_found.app.items.domain.ItemPostStatus;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import lost_and_found.app.items.service.ItemSearchCriteria;
import lost_and_found.app.items.service.ItemSearchCriteriaMapper;

class FilterMapperTest {

    private final ItemSearchCriteriaMapper criteriaMapper = new ItemSearchCriteriaMapper();

    @Test
    void fromQueryParams_shouldParseStatus() {
        ItemSearchCriteria criteria = criteriaMapper.fromQueryParams("open", null, null);

        assertEquals(ItemPostStatus.OPEN, criteria.status());
    }

    @Test
    void fromQueryParams_shouldNormalizeCategory() {
        ItemSearchCriteria criteria = criteriaMapper.fromQueryParams(null, " BAG ", null);

        assertEquals("BAG", criteria.category());
    }

    @Test
    void fromQueryParams_shouldIncludeUserId() {
        ItemSearchCriteria criteria = criteriaMapper.fromQueryParams(null, null, 1L);

        assertEquals(1L, criteria.userId());
    }

    @Test
    void fromQueryParams_shouldSupportCombinedCriteria() {
        ItemSearchCriteria criteria = criteriaMapper.fromQueryParams("resolved", "bag", 2L);

        assertEquals(ItemPostStatus.RESOLVED, criteria.status());
        assertEquals("bag", criteria.category());
        assertEquals(2L, criteria.userId());
    }

    @Test
    void fromQueryParams_shouldTreatBlankValuesAsMissing() {
        ItemSearchCriteria criteria = criteriaMapper.fromQueryParams(" ", " ", null);

        assertNull(criteria.status());
        assertNull(criteria.category());
        assertNull(criteria.userId());
    }

    @Test
    void fromQueryParams_shouldThrowForInvalidStatus() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
            criteriaMapper.fromQueryParams("invalid", null, null)
        );

        assertEquals("status must be one of: OPEN, RESOLVED.", error.getMessage());
    }
}
