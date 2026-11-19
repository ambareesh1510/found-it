package lost_and_found.app.shared.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Editable profile fields. Email is intentionally omitted: changing it would require a
 * re-verification flow (see task-004). {@code picture} may be null or blank to clear the photo.
 */
public record UpdateProfileDTO(
    @NotBlank(message = "Username is required.")
    @Size(max = 50, message = "Username must be at most 50 characters.")
    String username,
    String picture
) {

    /** Usernames are case-insensitive, so store and compare them in lowercase, trimmed. */
    public UpdateProfileDTO {
        username = username == null ? null : username.trim().toLowerCase();
        picture = picture == null || picture.isBlank() ? null : picture.trim();
    }
}
