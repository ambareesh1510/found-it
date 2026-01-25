package lost_and_found.app.auth.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import lost_and_found.app.auth.dto.ForgotPasswordDTO;
import lost_and_found.app.auth.dto.LoginDTO;
import lost_and_found.app.auth.dto.LoginResponseDTO;
import lost_and_found.app.auth.dto.RegisterDTO;
import lost_and_found.app.auth.dto.ResendVerificationDTO;
import lost_and_found.app.auth.dto.ResetPasswordDTO;
import lost_and_found.app.auth.dto.VerifyEmailDTO;
import lost_and_found.app.auth.service.AuthenticationService;
import lost_and_found.app.auth.service.BlacklistService;
import lost_and_found.app.auth.service.JwtService;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.util.CookieUtils;
import lost_and_found.app.shared.util.RequestTokens;

@Slf4j
@RestController
public class AuthController {

    private final long cookieExpireTimeSec;

    @Value("${security.jwt.is-prod}")
    boolean isProd;

    private static final String ACCESS_COOKIE_NAME = "access_token";

    private final AuthenticationService authenticationService;
    private final JwtService jwtService;
    private final BlacklistService blacklistService;

    public AuthController(
        AuthenticationService authenticationService,
        JwtService jwtService,
        BlacklistService blacklistService,
        @Value("${security.jwt.expiration-time}") long jwtExpireMs
    ) {
        this.authenticationService = authenticationService;
        this.jwtService = jwtService;
        this.blacklistService = blacklistService;
        this.cookieExpireTimeSec = jwtExpireMs / 1000L;
    }

    @GetMapping("/test")
    public String getTest() {
        return "Hello";
    }

    @PostMapping("/auth/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterDTO req) {
        User registeredUser = authenticationService.register(req);
        return ResponseEntity.ok(registeredUser.getUsername());
    }
    @PostMapping("/auth/verify")
    public ResponseEntity<String> verifyEmail(@Valid @RequestBody VerifyEmailDTO dto) {
        authenticationService.verifyEmail(dto.token());
        return ResponseEntity.ok("Email verified. You can now log in.");
    }

    @PostMapping("/auth/resend-verification")
    public ResponseEntity<String> resendVerification(@Valid @RequestBody ResendVerificationDTO dto) {
        authenticationService.resendVerification(dto.email());
        return ResponseEntity.ok("If that account is waiting for verification, a new link has been sent.");
    }

    @PostMapping("/auth/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordDTO dto) {
        authenticationService.forgotPassword(dto);
        return ResponseEntity.ok("If the account exists, a reset email has been sent.");
    }

    @PostMapping("/auth/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        authenticationService.resetPassword(dto.token(), dto.password());
        return ResponseEntity.ok("Your password has been reset. You can now log in.");
    }

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginDTO req) {
        LoginResponseDTO loginResponse = authenticationService.authenticate(req);
        ResponseCookie cookie = CookieUtils.addCookie(
            ACCESS_COOKIE_NAME, 
            loginResponse.token(), 
            cookieExpireTimeSec, 
            true, 
            isProd
        );
        return ResponseEntity
            .ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(loginResponse);
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String jwt = RequestTokens.resolve(request);

        if (jwt != null && !jwt.isBlank()) {
            try {
                String username = jwtService.extractUsername(jwt);
                String jti = jwtService.extractJti(jwt);
                if (jti != null) {
                    blacklistService.blacklistToken(jti, username);
                }
            } catch (Exception e) {
                log.error("Error logging out user with JWT " + jwt, e);
            }
        }

        ResponseCookie clearCookie = CookieUtils.deleteCookie(ACCESS_COOKIE_NAME, isProd);

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
            .body("Logged out");
    }
}
