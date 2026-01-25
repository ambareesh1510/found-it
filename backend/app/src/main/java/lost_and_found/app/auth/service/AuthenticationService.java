package lost_and_found.app.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import lost_and_found.app.auth.domain.AuthProviderType;
import lost_and_found.app.auth.dto.ForgotPasswordDTO;
import lost_and_found.app.auth.dto.LoginDTO;
import lost_and_found.app.auth.dto.LoginResponseDTO;
import lost_and_found.app.auth.dto.RegisterDTO;
import lost_and_found.app.exceptions.AccountVerificationError;
import lost_and_found.app.exceptions.InvalidTokenException;
import lost_and_found.app.exceptions.MalformedLoginRequestError;
import lost_and_found.app.exceptions.MalformedRegistrationRequestError;
import lost_and_found.app.exceptions.UserExistsException;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.infra.UserRepository;
import lost_and_found.app.shared.service.EmailService;

/**
 * Registration, email verification, login, and password reset.
 */
@Slf4j
@Service
public class AuthenticationService {

    static final String INCORRECT_CREDENTIALS = "Incorrect email or password.";
    static final String NOT_VERIFIED = "Account not verified. Please verify your account.";

    private static final Duration VERIFICATION_TTL = Duration.ofHours(24);
    private static final Duration RESET_TTL = Duration.ofHours(1);

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final EmailService emailService;

    public AuthenticationService(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            JwtService jwtService,
            EmailService emailService
    ) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    /**
     * Creates a disabled account and emails a verification link. The account
     * cannot log in until the link is used.
     */
    public User register(RegisterDTO input) throws UserExistsException, MalformedRegistrationRequestError {
        if (
                input.username() == null || input.username().isBlank() ||
                input.password() == null || input.password().isBlank() ||
                input.email() == null || input.email().isBlank()
        ) {
            throw new MalformedRegistrationRequestError("Registration request was malformed");
        }

        PasswordPolicy.requireValid(input.password());

        // Same person signing up again before verifying: resend the link instead of failing.
        // Only when username and password also match, so a stranger cannot take over the pending account.
        Optional<User> pending = userRepository.findByEmail(input.email())
                .filter(this::isPendingLocalAccount)
                .filter(u -> u.getUsername().equalsIgnoreCase(input.username()))
                .filter(u -> passwordEncoder.matches(input.password(), u.getPassword()));
        if (pending.isPresent()) {
            issueVerificationToken(pending.get());
            return pending.get();
        }

        if (userRepository.existsByUsernameOrEmail(input.username(), input.email())) {
            throw new UserExistsException("Email or username already exists");
        }

        String verificationToken = TokenUtil.newToken();
        User user = User.builder()
                .username(input.username())
                .password(passwordEncoder.encode(input.password()))
                .email(input.email())
                .authProviderType(AuthProviderType.USER_PASS)
                .enabled(false)
                .verificationCode(TokenUtil.hash(verificationToken))
                .verificationExpireTime(Instant.now().plus(VERIFICATION_TTL))
                .build();

        User saved = userRepository.save(user);
        sendVerificationEmailSafely(saved.getEmail(), verificationToken);
        return saved;
    }

    /**
     * Activates the account that owns the verification token. Tokens are single use.
     */
    public void verifyEmail(String token) {
        User user = findValidVerificationUser(token)
                .orElseThrow(() -> new InvalidTokenException("This verification link is invalid or has expired."));

        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationExpireTime(null);
        userRepository.save(user);
    }

    /**
     * Sends a fresh verification link if the account exists and is still unverified.
     * Callers get the same response either way, so this cannot be used to probe emails.
     */
    public void resendVerification(String email) {
        if (email == null || email.isBlank()) return;

        userRepository.findByEmail(email)
                .filter(this::isPendingLocalAccount)
                .ifPresent(this::issueVerificationToken);
    }

    /** An account that signed up with a password and has not verified its email yet. */
    private boolean isPendingLocalAccount(User user) {
        return !user.isEnabled() && user.getAuthProviderType() == AuthProviderType.USER_PASS;
    }

    /** Replaces any previous verification token, saves the hash, and emails the new link. */
    private void issueVerificationToken(User user) {
        String token = TokenUtil.newToken();
        user.setVerificationCode(TokenUtil.hash(token));
        user.setVerificationExpireTime(Instant.now().plus(VERIFICATION_TTL));
        userRepository.save(user);
        sendVerificationEmailSafely(user.getEmail(), token);
    }

    /**
     * Emails a reset link. Always returns normally so callers cannot learn which emails exist.
     */
    public void forgotPassword(ForgotPasswordDTO input) {
        if (input.email() == null || input.email().isBlank()) return;

        userRepository.findByEmail(input.email())
                .filter(u -> u.getAuthProviderType() != AuthProviderType.GOOGLE)
                .ifPresent(user -> {
                    String token = TokenUtil.newToken();
                    user.setResetToken(TokenUtil.hash(token));
                    user.setResetTokenExpiry(Instant.now().plus(RESET_TTL));
                    userRepository.save(user);

                    try {
                        emailService.sendResetEmail(user.getEmail(), token);
                    } catch (Exception e) {
                        log.error("Failed to send password reset email", e);
                    }
                });
    }

    /**
     * Sets a new password for the account that owns the reset token. Tokens are single use.
     * Using the link also proves control of the inbox, so an unverified account is enabled.
     */
    public void resetPassword(String token, String newPassword) {
        PasswordPolicy.requireValid(newPassword);

        User user = findValidResetUser(token)
                .orElseThrow(() -> new InvalidTokenException("This reset link is invalid or has expired."));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.setEnabled(true);
        userRepository.save(user);
    }

    /**
     * Checks the password before revealing whether the account is verified, so an
     * attacker cannot learn which emails are registered without the password.
     */
    public LoginResponseDTO authenticate(LoginDTO input) throws MalformedLoginRequestError {
        if (
                input.usernameOrEmail() == null || input.usernameOrEmail().isBlank() ||
                input.password() == null || input.password().isBlank()
        ) {
            throw new MalformedLoginRequestError("Login request was malformed");
        }

        UserDetails user;
        try {
            user = userDetailsService.loadUserByUsername(input.usernameOrEmail());
        } catch (UsernameNotFoundException e) {
            throw new BadCredentialsException(INCORRECT_CREDENTIALS);
        }

        if (user.getPassword() == null || !passwordEncoder.matches(input.password(), user.getPassword())) {
            throw new BadCredentialsException(INCORRECT_CREDENTIALS);
        }

        if (!user.isEnabled()) {
            throw new AccountVerificationError(NOT_VERIFIED);
        }

        String jwt = jwtService.generateToken(user);
        return new LoginResponseDTO(jwt, user.getUsername());
    }

    private Optional<User> findValidVerificationUser(String token) {
        if (token == null || token.isBlank()) return Optional.empty();

        return userRepository.findByVerificationCode(TokenUtil.hash(token))
                .filter(u -> u.getVerificationExpireTime() != null
                        && Instant.now().isBefore(u.getVerificationExpireTime()));
    }

    private Optional<User> findValidResetUser(String token) {
        if (token == null || token.isBlank()) return Optional.empty();

        return userRepository.findByResetToken(TokenUtil.hash(token))
                .filter(u -> u.getResetTokenExpiry() != null
                        && Instant.now().isBefore(u.getResetTokenExpiry()));
    }

    /**
     * Registration succeeds even if the mail server is down; the user can request
     * another link from the login screen.
     */
    private void sendVerificationEmailSafely(String email, String token) {
        try {
            emailService.sendVerificationEmail(email, token);
        } catch (Exception e) {
            log.error("Failed to send verification email", e);
        }
    }
}
