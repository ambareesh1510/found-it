package lost_and_found.app.searches.infra;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lost_and_found.app.exceptions.UserNotFoundException;
import lost_and_found.app.searches.dto.request.LostSearchRequestDTO;
import lost_and_found.app.searches.dto.response.LostSearchResultDTO;
import lost_and_found.app.searches.service.LostSearchService;
import lost_and_found.app.shared.domain.User;

@RestController
@RequestMapping("/searches")
public class LostSearchController {

    private final LostSearchService lostSearchService;

    public LostSearchController(LostSearchService lostSearchService) {
        this.lostSearchService = lostSearchService;
    }

    /** Declares a lost search and returns matching FOUND posts in one call. Requires login. */
    @PostMapping("/lost")
    public ResponseEntity<LostSearchResultDTO> searchLost(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody LostSearchRequestDTO req
    ) {
        if (user == null) throw new UserNotFoundException("User Not Authenticated");
        return ResponseEntity.ok(lostSearchService.searchFoundItems(user.getId(), req));
    }
}
