package lost_and_found.app.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationDTO(
    @NotBlank(message = "Email is required.")
    @Email(message = "Email must be valid.")
    String email
) {

    public ResendVerificationDTO {
        email = Normalize.lower(email);
    }
}
