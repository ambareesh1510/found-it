package lost_and_found.app.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * An identity token from a native sign-in (Google or Apple).
 *
 * @param authorizationCode Apple only: the one-time code from the same sign-in. Optional, because
 *        older app builds do not send it; without it the Apple link cannot be revoked on deletion.
 */
public record IdTokenLoginDTO(
    @NotBlank(message = "Sign-in token is required.")
    String idToken,
    String authorizationCode
) {
}
