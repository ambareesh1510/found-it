package lost_and_found.app.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginDTO(
    @NotBlank(message = "Username or email is required.")
    String usernameOrEmail,
    @NotBlank(message = "Password is required.")
    String password
) {

    /** Usernames and emails are case-insensitive, so store and compare them in lowercase. */
    public LoginDTO {
        usernameOrEmail = Normalize.lower(usernameOrEmail);
    }
}
