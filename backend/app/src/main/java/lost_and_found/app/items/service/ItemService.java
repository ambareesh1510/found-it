package lost_and_found.app.items.service;

import java.util.List;

import lost_and_found.app.exceptions.ItemNotFoundException;
import lost_and_found.app.exceptions.UserMismatchException;
import lost_and_found.app.exceptions.UserNotFoundException;
import lost_and_found.app.items.dto.request.CreateItemDTO;
import lost_and_found.app.items.dto.request.UpdateItemDTO;
import lost_and_found.app.items.dto.response.ItemPostResponseDTO;

/**
 * An interface that defines the business logic for item posts.
 */
public interface ItemService {
    /**
     * Creates a new item post.
     * @param userId the user who requested the creation.
     * @param req the req dto for the creation.
     * @return the responseDTO for the creation action.
     * @throws UserNotFoundException when a non existent user tries to create a post.
     */
    ItemPostResponseDTO createItem(Long userId, CreateItemDTO req) throws UserNotFoundException;
    /**
     * A general update to an existing item post.
     * @param userId the user who requested the update.
     * @param req the req dto containing the details for the update.
     * @return the updated post in dto form.
     * @throws UserMismatchException when a user tries to modify an item that does not belong to them.
     * @throws ItemNotFoundException when the user tries to modify a nonexistent post.
     */
    ItemPostResponseDTO updateItem(Long userId, UpdateItemDTO req) throws ItemNotFoundException, UserMismatchException;
    /**
     * Deletes an item post.
     * @param userId the user who requested the deletion.
     * @param itemId the item to be deleted.
     * @throws UserMismatchException when a user tries to modify an item that does not belong to them.
     * @throws ItemNotFoundException when the user tries to delete a nonexistent post.
     */
    void deleteItem(Long userId, Long itemId) throws ItemNotFoundException, UserMismatchException;
    /**
     * Gets items near a certain latitude and longitude.
     * @param viewerId the requesting user, or null for anonymous. Controls detailLocation visibility.
     * @param lon the longitude of the search point.
     * @param lat the latitude of the search point.
     * @param radiusMeters radius of search with the search point as the center.
     * @return all items in the search area.
     */
    List<ItemPostResponseDTO> getNearbyItems(Long viewerId, double lon, double lat, double radiusMeters);
    /**
     * Gets all items in correspondence with the search criteria.
     * @param viewerId the requesting user, or null for anonymous. Controls detailLocation visibility.
     * @param criteria the criteria to be applied to the get.
     * @return all items that match the filter criteria.
     */
    List<ItemPostResponseDTO> getItems(Long viewerId, ItemSearchCriteria criteria);
    /**
     * Get item post by id.
     * @param viewerId the requesting user, or null for anonymous. Controls detailLocation visibility.
     * @param itemId the item id.
     * @return the item with id [itemId].
     * @throws ItemNotFoundException when the item does not exist.
     */
    ItemPostResponseDTO getItemById(Long viewerId, Long itemId) throws ItemNotFoundException;

    /**
     * Confirms, as the chosen counterparty, a resolution the poster proposed. When both sides have
     * confirmed, the item becomes RESOLVED.
     * @param userId the user confirming; must be the item's {@code resolvedWithUser}.
     * @param itemId the item whose pending resolution is being confirmed.
     * @return the updated item.
     * @throws ItemNotFoundException when the item does not exist.
     * @throws UserMismatchException when the caller is not the chosen counterparty.
     * @throws IllegalStateException when the item has no resolution awaiting this user.
     */
    ItemPostResponseDTO confirmResolution(Long userId, Long itemId)
        throws ItemNotFoundException, UserMismatchException;
}
