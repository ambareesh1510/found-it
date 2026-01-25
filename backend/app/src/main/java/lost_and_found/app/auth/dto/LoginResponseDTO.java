package lost_and_found.app.auth.dto;

public record LoginResponseDTO(
    String token,
    String username
) {
}
