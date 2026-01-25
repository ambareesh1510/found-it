package lost_and_found.app.searches.dto.response;

import java.util.List;

public record LostSearchResultDTO(
    Long searchId,
    List<FoundItemSummaryDTO> results
) {}
