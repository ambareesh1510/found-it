package lost_and_found.app.shared.dto;

import java.time.Instant;

import lost_and_found.app.auth.domain.AuthProviderType;

public record CurrentUserResponseDTO(
    Long id,
    String username,
    String email,
    boolean enabled,
    String picture,
    AuthProviderType authProviderType,
    Instant createdAt,
    Instant updatedAt
) {
}
