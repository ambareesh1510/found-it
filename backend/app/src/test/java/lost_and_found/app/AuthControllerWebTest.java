package lost_and_found.app;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import lost_and_found.app.auth.infra.AuthController;
import lost_and_found.app.auth.service.AuthenticationService;
import lost_and_found.app.auth.service.BlacklistService;
import lost_and_found.app.auth.service.HS256JwtService;
import lost_and_found.app.auth.service.JwtService;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.infra.GlobalExceptionHandler;
import lost_and_found.app.test_implementations.BlacklistServiceTest;
import lost_and_found.app.test_implementations.EmailServiceTest;
import lost_and_found.app.test_implementations.PasswordEncoderTest;
import lost_and_found.app.test_implementations.UserDetailsServiceTest;
import lost_and_found.app.test_implementations.UserRepositoryTest;

class AuthControllerWebTest {

    private static final String VALID_PASSWORD = "Pass1234567!";

    private MockMvc mockMvc;
    private UserRepositoryTest userRepository;
    private PasswordEncoder passwordEncoder;
    private EmailServiceTest emailService;

    @BeforeEach
    void setUp() {
        byte[] keyBytes = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
            .getBytes(StandardCharsets.UTF_8);
        String secretKeyB64 = Base64.getEncoder().encodeToString(keyBytes);

        userRepository = new UserRepositoryTest();
        passwordEncoder = new PasswordEncoderTest();
        emailService = new EmailServiceTest();
        BlacklistService blacklistService = new BlacklistServiceTest();
        JwtService jwtService = new HS256JwtService(blacklistService, secretKeyB64, 60_000L);
        AuthenticationService authenticationService = new AuthenticationService(
            new UserDetailsServiceTest(userRepository),
            passwordEncoder,
            userRepository,
            jwtService,
            emailService
        );

        mockMvc = MockMvcBuilders
            .standaloneSetup(new AuthController(authenticationService, jwtService, blacklistService, 60_000L))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void register_shouldReturnBadRequestForMissingEmail() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "saket",
                      "password": "Pass1234567!"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message", containsString("Email is required.")));
    }

    @Test
    void register_shouldReturnBadRequestForWeakPassword() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "saket",
                      "password": "short",
                      "email": "saket@test.com"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("at least 12 characters")));
    }

    @Test
    void register_shouldReturnConflictForDuplicateUser() throws Exception {
        String body = """
            {
              "username": "saket",
              "password": "Pass1234567!",
              "email": "saket@test.com"
            }
            """;
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk());

        // Same email with a different password is a real conflict. (Same credentials before
        // verifying just resends the link; see AuthenticationServiceTest.)
        String otherPassword = """
            {
              "username": "saket",
              "password": "Other9999999!",
              "email": "saket@test.com"
            }
            """;
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(otherPassword))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Email or username already exists"));
    }

    @Test
    void login_shouldReturnBadRequestForMissingPassword() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "usernameOrEmail": "saket@test.com"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message", containsString("Password is required.")));
    }

    @Test
    void login_shouldReturnUnauthorizedForWrongPassword() throws Exception {
        userRepository.save(User.builder()
            .username("saket")
            .password(passwordEncoder.encode(VALID_PASSWORD))
            .email("saket@test.com")
            .enabled(true)
            .build());

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "usernameOrEmail": "saket@test.com",
                      "password": "WrongPass123!"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Incorrect email or password."));
    }

    @Test
    void login_shouldReturnForbiddenForUnverifiedAccount() throws Exception {
        userRepository.save(User.builder()
            .username("saket")
            .password(passwordEncoder.encode(VALID_PASSWORD))
            .email("saket@test.com")
            .enabled(false)
            .build());

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "usernameOrEmail": "saket@test.com",
                      "password": "Pass1234567!"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Account Not Verified"))
            .andExpect(jsonPath("$.message").value("Account not verified. Please verify your account."));
    }

    @Test
    void verify_shouldActivateAccount_fromEmailedToken() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {
                  "username": "saket",
                  "password": "Pass1234567!",
                  "email": "saket@test.com"
                }
                """))
            .andExpect(status().isOk());

        String token = emailService.lastVerificationToken("saket@test.com");
        mockMvc.perform(post("/auth/verify").contentType(MediaType.APPLICATION_JSON).content("""
                {
                  "token": "%s"
                }
                """.formatted(token)))
            .andExpect(status().isOk());

        org.junit.jupiter.api.Assertions.assertTrue(
            userRepository.findByEmail("saket@test.com").orElseThrow().isEnabled()
        );
    }

    @Test
    void verify_shouldReturnBadRequestForUnknownToken() throws Exception {
        mockMvc.perform(post("/auth/verify").contentType(MediaType.APPLICATION_JSON).content("""
                {
                  "token": "made-up-token"
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("invalid or has expired")));
    }

    @Test
    void resetPassword_shouldReturnBadRequestForWeakPassword() throws Exception {
        mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content("""
                {
                  "token": "made-up-token",
                  "password": "weakpassword"
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("uppercase")));
    }
}
