package lost_and_found.app.auth.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class AuthDtoNormalizationTest {

    @Test
    void registerLowercasesAndTrimsUsernameAndEmail() {
        RegisterDTO dto = new RegisterDTO("  Saket_Dev ", "Password1!", "  Saket@Test.COM ");

        assertEquals("saket_dev", dto.username());
        assertEquals("saket@test.com", dto.email());
        assertEquals("Password1!", dto.password());
    }

    @Test
    void loginLowercasesUsernameOrEmail() {
        LoginDTO dto = new LoginDTO("Saket@Test.com", "Password1!");

        assertEquals("saket@test.com", dto.usernameOrEmail());
    }

    @Test
    void emailOnlyDtosLowercaseEmail() {
        assertEquals("a@b.co", new ResendVerificationDTO("A@B.CO").email());
        assertEquals("a@b.co", new ForgotPasswordDTO("A@B.co").email());
    }

    @Test
    void nullValuesStayNullSoValidationStillReportsThem() {
        assertNull(new RegisterDTO(null, "Password1!", null).username());
        assertNull(new RegisterDTO(null, "Password1!", null).email());
        assertNull(new LoginDTO(null, "Password1!").usernameOrEmail());
    }
}
