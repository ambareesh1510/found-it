package lost_and_found.app.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordDTO(
    @NotBlank(message = "Reset token is required.")
    String token,
    @NotBlank(message = "Password is required.")
    String password
) {}
