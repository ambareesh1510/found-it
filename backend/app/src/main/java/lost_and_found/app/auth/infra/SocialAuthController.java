package lost_and_found.app.auth.infra;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lost_and_found.app.auth.dto.IdTokenLoginDTO;
import lost_and_found.app.auth.dto.LoginResponseDTO;
import lost_and_found.app.auth.service.SocialAuthService;

/** Sign-in endpoints for the native iOS app. The web app keeps using the browser OAuth flow. */
@RestController
public class SocialAuthController {

    private final SocialAuthService socialAuthService;

    public SocialAuthController(SocialAuthService socialAuthService) {
        this.socialAuthService = socialAuthService;
    }

    @PostMapping("/auth/google")
    public ResponseEntity<LoginResponseDTO> loginWithGoogle(@Valid @RequestBody IdTokenLoginDTO req) {
        return ResponseEntity.ok(socialAuthService.loginWithGoogle(req.idToken()));
    }

    @PostMapping("/auth/apple")
    public ResponseEntity<LoginResponseDTO> loginWithApple(@Valid @RequestBody IdTokenLoginDTO req) {
        return ResponseEntity.ok(socialAuthService.loginWithApple(req.idToken(), req.authorizationCode()));
    }
}
