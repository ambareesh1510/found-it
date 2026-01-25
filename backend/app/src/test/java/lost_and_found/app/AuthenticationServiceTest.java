package lost_and_found.app;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import lost_and_found.app.auth.dto.ForgotPasswordDTO;
import lost_and_found.app.auth.dto.LoginDTO;
import lost_and_found.app.auth.dto.LoginResponseDTO;
import lost_and_found.app.auth.dto.RegisterDTO;
import lost_and_found.app.auth.service.AuthenticationService;
import lost_and_found.app.auth.service.HS256JwtService;
import lost_and_found.app.auth.service.JwtService;
import lost_and_found.app.auth.service.TokenUtil;
import lost_and_found.app.exceptions.AccountVerificationError;
import lost_and_found.app.exceptions.InvalidTokenException;
import lost_and_found.app.exceptions.MalformedRegistrationRequestError;
import lost_and_found.app.exceptions.UserExistsException;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.test_implementations.BlacklistServiceTest;
import lost_and_found.app.test_implementations.EmailServiceTest;
import lost_and_found.app.test_implementations.PasswordEncoderTest;
import lost_and_found.app.test_implementations.UserDetailsServiceTest;
import lost_and_found.app.test_implementations.UserRepositoryTest;

class AuthenticationServiceTest {

    private static final String VALID_PASSWORD = "Pass1234567!";
    private static final String NEW_PASSWORD = "NewPass7890!!";

    private AuthenticationService authenticationService;
    private PasswordEncoder passwordEncoder;
    private UserRepositoryTest userRepository;
    private EmailServiceTest emailService;

    @BeforeEach
    void setUp() {
        byte[] keyBytes = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
                .getBytes(StandardCharsets.UTF_8);
        String secretKeyB64 = Base64.getEncoder().encodeToString(keyBytes);

        userRepository = new UserRepositoryTest();
        passwordEncoder = new PasswordEncoderTest();
        emailService = new EmailServiceTest();
        JwtService jwtService = new HS256JwtService(new BlacklistServiceTest(), secretKeyB64, 60_000L);

        authenticationService = new AuthenticationService(
            new UserDetailsServiceTest(userRepository),
            passwordEncoder,
            userRepository,
            jwtService,
            emailService
        );
    }

    /** Registers an account and uses the emailed token to verify it. */
    private User registerAndVerify(String username, String email, String password) {
        User saved = authenticationService.register(new RegisterDTO(username, password, email));
        authenticationService.verifyEmail(emailService.lastVerificationToken(email));
        return saved;
    }

    // ---------- Registration ----------

    @Test
    void register_shouldCreateDisabledUser_andSendVerificationEmail() {
        User saved = authenticationService.register(new RegisterDTO("saket", VALID_PASSWORD, "saket@test.com"));

        assertNotNull(saved.getId());
        assertFalse(saved.isEnabled());
        assertNotNull(emailService.lastVerificationToken("saket@test.com"));
        assertNotNull(saved.getVerificationExpireTime());
    }

    @Test
    void register_shouldStoreHashOfVerificationToken_notRawToken() {
        User saved = authenticationService.register(new RegisterDTO("saket", VALID_PASSWORD, "saket@test.com"));
        String rawToken = emailService.lastVerificationToken("saket@test.com");

        assertEquals(TokenUtil.hash(rawToken), saved.getVerificationCode());
        assertNotEquals(rawToken, saved.getVerificationCode());
    }

    @Test
    void register_shouldEncodePassword() {
        User saved = authenticationService.register(new RegisterDTO("saket", VALID_PASSWORD, "saket@test.com"));

        assertNotEquals(VALID_PASSWORD, saved.getPassword());
        assertTrue(passwordEncoder.matches(VALID_PASSWORD, saved.getPassword()));
    }

    @Test
    void register_shouldThrowWhenPasswordTooShort() {
        RuntimeException ex = assertThrows(MalformedRegistrationRequestError.class, () ->
            authenticationService.register(new RegisterDTO("saket", "Sh0rt!", "saket@test.com"))
        );

        assertTrue(ex.getMessage().contains("at least 12 characters"));
    }

    @Test
    void register_shouldRequireUppercaseLowercaseAndSpecial() {
        String noUpper = "pass1234567!";
        String noLower = "PASS1234567!";
        String noSpecial = "Pass12345678";

        assertTrue(registrationError(noUpper).contains("uppercase"));
        assertTrue(registrationError(noLower).contains("lowercase"));
        assertTrue(registrationError(noSpecial).contains("special character"));
    }

    private String registrationError(String password) {
        MalformedRegistrationRequestError ex = assertThrows(MalformedRegistrationRequestError.class, () ->
            authenticationService.register(new RegisterDTO("saket", password, "saket@test.com"))
        );
        return ex.getMessage();
    }

    @Test
    void register_shouldRejectDuplicateEmailOrUsername() {
        authenticationService.register(new RegisterDTO("saket", VALID_PASSWORD, "saket@test.com"));

        assertThrows(UserExistsException.class, () ->
            authenticationService.register(new RegisterDTO("saket", VALID_PASSWORD, "other@test.com"))
        );
    }

    @Test
    void register_again_beforeVerifying_sameCredentials_resendsLink() {
        authenticationService.register(new RegisterDTO("saket", VALID_PASSWORD, "saket@test.com"));
        String firstToken = emailService.lastVerificationToken("saket@test.com");

        assertDoesNotThrow(() ->
            authenticationService.register(new RegisterDTO("saket", VALID_PASSWORD, "saket@test.com"))
        );
        String secondToken = emailService.lastVerificationToken("saket@test.com");

        assertNotEquals(firstToken, secondToken);
        assertThrows(InvalidTokenException.class, () -> authenticationService.verifyEmail(firstToken));
        authenticationService.verifyEmail(secondToken);
        assertTrue(userRepository.findByEmail("saket@test.com").orElseThrow().isEnabled());
    }

    @Test
    void register_again_beforeVerifying_differentPassword_isRejected() {
        authenticationService.register(new RegisterDTO("saket", VALID_PASSWORD, "saket@test.com"));

        assertThrows(UserExistsException.class, () ->
            authenticationService.register(new RegisterDTO("saket", "Other9999999!", "saket@test.com"))
        );
    }

    @Test
    void register_again_afterVerifying_isRejected() {
        registerAndVerify("saket", "saket@test.com", VALID_PASSWORD);

        assertThrows(UserExistsException.class, () ->
            authenticationService.register(new RegisterDTO("saket", VALID_PASSWORD, "saket@test.com"))
        );
    }

    @Test
    void register_multipleUsers_shouldGetDifferentIds() {
        User u1 = authenticationService.register(new RegisterDTO("a", VALID_PASSWORD, "a@test.com"));
        User u2 = authenticationService.register(new RegisterDTO("b", VALID_PASSWORD, "b@test.com"));

        assertNotEquals(u1.getId(), u2.getId());
    }

    // ---------- Email verification ----------

    @Test
    void verifyEmail_shouldEnableAccount_andClearToken() {
        authenticationService.register(new RegisterDTO("saket", VALID_PASSWORD, "saket@test.com"));
        authenticationService.verifyEmail(emailService.lastVerificationToken("saket@test.com"));

        User user = userRepository.findByEmail("saket@test.com").orElseThrow();
        assertTrue(user.isEnabled());
        assertNull(user.getVerificationCode());
        assertNull(user.getVerificationExpireTime());
    }

    @Test
    void verifyEmail_shouldRejectUnknownToken() {
        assertThrows(InvalidTokenException.class, () -> authenticationService.verifyEmail("not-a-real-token"));
    }

    @Test
    void verifyEmail_shouldRejectTokenUsedTwice() {
        authenticationService.register(new RegisterDTO("saket", VALID_PASSWORD, "saket@test.com"));
        String token = emailService.lastVerificationToken("saket@test.com");
        authenticationService.verifyEmail(token);

        assertThrows(InvalidTokenException.class, () -> authenticationService.verifyEmail(token));
    }

    @Test
    void verifyEmail_shouldRejectExpiredToken() {
        authenticationService.register(new RegisterDTO("saket", VALID_PASSWORD, "saket@test.com"));
        User user = userRepository.findByEmail("saket@test.com").orElseThrow();
        user.setVerificationExpireTime(Instant.now().minusSeconds(1));
        userRepository.save(user);

        assertThrows(InvalidTokenException.class, () ->
            authenticationService.verifyEmail(emailService.lastVerificationToken("saket@test.com"))
        );
    }

    @Test
    void resendVerification_shouldReplaceOldToken() {
        authenticationService.register(new RegisterDTO("saket", VALID_PASSWORD, "saket@test.com"));
        String oldToken = emailService.lastVerificationToken("saket@test.com");

        authenticationService.resendVerification("saket@test.com");
        String newToken = emailService.lastVerificationToken("saket@test.com");

        assertNotEquals(oldToken, newToken);
        assertThrows(InvalidTokenException.class, () -> authenticationService.verifyEmail(oldToken));
        authenticationService.verifyEmail(newToken);
        assertTrue(userRepository.findByEmail("saket@test.com").orElseThrow().isEnabled());
    }

    @Test
    void resendVerification_shouldDoNothing_forUnknownEmail() {
        assertDoesNotThrow(() -> authenticationService.resendVerification("nobody@test.com"));
        assertNull(emailService.lastVerificationToken("nobody@test.com"));
    }

    // ---------- Login ----------

    @Test
    void authenticate_shouldReturnToken_whenVerified_usingUsername() {
        registerAndVerify("saket", "saket@test.com", VALID_PASSWORD);

        LoginResponseDTO result = authenticationService.authenticate(new LoginDTO("saket", VALID_PASSWORD));

        assertEquals("saket", result.username());
        assertNotNull(result.token());
    }

    @Test
    void authenticate_shouldReturnToken_whenVerified_usingEmail() {
        registerAndVerify("saket", "saket@test.com", VALID_PASSWORD);

        LoginResponseDTO result = authenticationService.authenticate(new LoginDTO("saket@test.com", VALID_PASSWORD));

        assertEquals("saket", result.username());
    }

    @Test
    void authenticate_shouldThrowAccountVerificationError_whenCorrectPasswordButUnverified() {
        authenticationService.register(new RegisterDTO("saket", VALID_PASSWORD, "saket@test.com"));

        AccountVerificationError ex = assertThrows(AccountVerificationError.class, () ->
            authenticationService.authenticate(new LoginDTO("saket@test.com", VALID_PASSWORD))
        );

        assertEquals("Account not verified. Please verify your account.", ex.getMessage());
    }

    @Test
    void authenticate_shouldThrowBadCredentials_whenWrongPassword_evenIfUnverified() {
        // Wrong password must not reveal that the account exists but is unverified.
        authenticationService.register(new RegisterDTO("saket", VALID_PASSWORD, "saket@test.com"));

        assertThrows(BadCredentialsException.class, () ->
            authenticationService.authenticate(new LoginDTO("saket@test.com", "WrongPass123!"))
        );
    }

    @Test
    void authenticate_shouldThrowBadCredentials_whenUserDoesNotExist() {
        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () ->
            authenticationService.authenticate(new LoginDTO("missingUser", VALID_PASSWORD))
        );

        assertEquals("Incorrect email or password.", ex.getMessage());
    }

    @Test
    void authenticate_shouldThrowWhenRequestMalformed() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            authenticationService.authenticate(new LoginDTO("", ""))
        );

        assertEquals("Login request was malformed", ex.getMessage());
    }

    // ---------- Forgot / reset password ----------

    @Test
    void forgotPassword_shouldSendResetToken_andStoreOnlyItsHash() {
        registerAndVerify("saket", "saket@test.com", VALID_PASSWORD);

        authenticationService.forgotPassword(new ForgotPasswordDTO("saket@test.com"));

        String rawToken = emailService.lastResetToken("saket@test.com");
        assertNotNull(rawToken);
        User user = userRepository.findByEmail("saket@test.com").orElseThrow();
        assertEquals(TokenUtil.hash(rawToken), user.getResetToken());
        assertNotNull(user.getResetTokenExpiry());
    }

    @Test
    void forgotPassword_shouldNotThrow_forUnknownEmail() {
        assertDoesNotThrow(() -> authenticationService.forgotPassword(new ForgotPasswordDTO("nobody@test.com")));
        assertNull(emailService.lastResetToken("nobody@test.com"));
    }

    @Test
    void resetPassword_shouldChangePassword_andTokenIsSingleUse() {
        registerAndVerify("saket", "saket@test.com", VALID_PASSWORD);
        authenticationService.forgotPassword(new ForgotPasswordDTO("saket@test.com"));
        String token = emailService.lastResetToken("saket@test.com");

        authenticationService.resetPassword(token, NEW_PASSWORD);

        assertNotNull(authenticationService.authenticate(new LoginDTO("saket", NEW_PASSWORD)));
        assertThrows(BadCredentialsException.class, () ->
            authenticationService.authenticate(new LoginDTO("saket", VALID_PASSWORD))
        );
        assertThrows(InvalidTokenException.class, () -> authenticationService.resetPassword(token, NEW_PASSWORD));
    }

    @Test
    void resetPassword_shouldRejectWeakPassword_withoutConsumingToken() {
        registerAndVerify("saket", "saket@test.com", VALID_PASSWORD);
        authenticationService.forgotPassword(new ForgotPasswordDTO("saket@test.com"));
        String token = emailService.lastResetToken("saket@test.com");

        assertThrows(MalformedRegistrationRequestError.class, () ->
            authenticationService.resetPassword(token, "weakpassword")
        );
        assertDoesNotThrow(() -> authenticationService.resetPassword(token, NEW_PASSWORD));
    }

    @Test
    void resetPassword_shouldRejectExpiredToken() {
        registerAndVerify("saket", "saket@test.com", VALID_PASSWORD);
        authenticationService.forgotPassword(new ForgotPasswordDTO("saket@test.com"));
        String token = emailService.lastResetToken("saket@test.com");
        User user = userRepository.findByEmail("saket@test.com").orElseThrow();
        user.setResetTokenExpiry(Instant.now().minusSeconds(1));
        userRepository.save(user);

        assertThrows(InvalidTokenException.class, () -> authenticationService.resetPassword(token, NEW_PASSWORD));
    }
}
