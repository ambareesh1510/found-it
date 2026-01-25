package lost_and_found.app.items.infra;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import lost_and_found.app.exceptions.UserNotFoundException;
import lost_and_found.app.items.dto.request.CreateItemDTO;
import lost_and_found.app.items.dto.request.UpdateItemDTO;
import lost_and_found.app.items.dto.response.ItemPostResponseDTO;
import lost_and_found.app.items.service.ItemSearchCriteria;
import lost_and_found.app.items.service.ItemSearchCriteriaMapper;
import lost_and_found.app.items.service.ItemService;
import lost_and_found.app.shared.domain.User;

@RestController
@RequestMapping("/items")
public class ItemController {

    private final ItemService itemService;
    private final ItemSearchCriteriaMapper criteriaMapper;

    public ItemController(ItemService itemService, ItemSearchCriteriaMapper criteriaMapper) {
        this.itemService = itemService;
        this.criteriaMapper = criteriaMapper;
    }

    @PostMapping
    public ResponseEntity<ItemPostResponseDTO> createItem(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody CreateItemDTO req
    ) {
        if (user == null) throw new UserNotFoundException("User Not Authenticated");
        return ResponseEntity.ok(itemService.createItem(user.getId(), req));
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<ItemPostResponseDTO> updateItem(
        @AuthenticationPrincipal User user,
        @PathVariable Long itemId,
        @Valid @RequestBody UpdateItemDTO req
    ) {
        if (user == null) throw new UserNotFoundException("User Not Authenticated");

        if (!itemId.equals(req.itemId())) {
            throw new IllegalArgumentException("Path itemId must match request itemId.");
        }

        return ResponseEntity.ok(itemService.updateItem(user.getId(), req));
    }

    /** Lets the chosen counterparty confirm a resolution the poster proposed. */
    @PostMapping("/{itemId}/confirm-resolution")
    public ResponseEntity<ItemPostResponseDTO> confirmResolution(
        @AuthenticationPrincipal User user,
        @PathVariable Long itemId
    ) {
        if (user == null) throw new UserNotFoundException("User Not Authenticated");
        return ResponseEntity.ok(itemService.confirmResolution(user.getId(), itemId));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(
        @AuthenticationPrincipal User user,
        @PathVariable Long itemId
    ) {
        if (user == null) throw new UserNotFoundException("User Not Authenticated");
        itemService.deleteItem(user.getId(), itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<ItemPostResponseDTO> getItemById(
        @AuthenticationPrincipal User user,
        @PathVariable Long itemId
    ) {
        return ResponseEntity.ok(itemService.getItemById(viewerId(user), itemId));
    }

    @GetMapping
    public ResponseEntity<List<ItemPostResponseDTO>> getItems(
        @AuthenticationPrincipal User user,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) String postType
    ) {
        ItemSearchCriteria criteria = criteriaMapper.fromQueryParams(
            status,
            category,
            userId,
            postType
        );
        return ResponseEntity.ok(itemService.getItems(viewerId(user), criteria));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<ItemPostResponseDTO>> getNearbyItems(
        @AuthenticationPrincipal User user,
        @RequestParam double lon,
        @RequestParam double lat,
        @RequestParam double radiusMeters
    ) {
        return ResponseEntity.ok(itemService.getNearbyItems(viewerId(user), lon, lat, radiusMeters));
    }

    /** Null for anonymous callers, which hides detailLocation from every item. */
    private static Long viewerId(User user) {
        return user == null ? null : user.getId();
    }
}
