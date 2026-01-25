package lost_and_found.app.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Password strength is enforced by PasswordPolicy in the service layer so the
 * message is identical for registration and password reset.
 */
public record RegisterDTO(
    @NotBlank(message = "Username is required.")
    String username,
    @NotBlank(message = "Password is required.")
    String password,
    @NotBlank(message = "Email is required.")
    @Email(message = "Email must be valid.")
    String email
) {

    /** Usernames and emails are case-insensitive, so store and compare them in lowercase. */
    public RegisterDTO {
        username = Normalize.lower(username);
        email = Normalize.lower(email);
    }
}
