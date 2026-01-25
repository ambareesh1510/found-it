package lost_and_found.app.auth.dto;

public record ForgotPasswordDTO(
    String email
) {

    public ForgotPasswordDTO {
        email = Normalize.lower(email);
    }
}
