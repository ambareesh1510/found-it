package lost_and_found.app.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailDTO(
    @NotBlank(message = "Verification token is required.")
    String token
) {}
