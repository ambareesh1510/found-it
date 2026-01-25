package lost_and_found.app;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import lost_and_found.app.auth.infra.SocialAuthController;
import lost_and_found.app.auth.service.AuthUtil;
import lost_and_found.app.auth.service.HS256JwtService;
import lost_and_found.app.auth.service.JwtService;
import lost_and_found.app.auth.service.OAuth2AuthenticationService;
import lost_and_found.app.auth.service.SocialAuthService;
import lost_and_found.app.auth.service.SocialIdTokenVerifier;
import lost_and_found.app.shared.infra.GlobalExceptionHandler;
import lost_and_found.app.test_implementations.BlacklistServiceTest;
import lost_and_found.app.test_implementations.FakeAppleTokens;
import lost_and_found.app.test_implementations.TestIdTokens;
import lost_and_found.app.test_implementations.UserRepositoryTest;

class SocialAuthControllerWebTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        String secret = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
        UserRepositoryTest users = new UserRepositoryTest();
        JwtService jwtService = new HS256JwtService(new BlacklistServiceTest(), secret, 60_000L);
        AuthUtil authUtil = new AuthUtil(users);
        SocialIdTokenVerifier verifier = new SocialIdTokenVerifier(
            TestIdTokens.decoderFor(TestIdTokens.newRsaKeyPair()),
            TestIdTokens.decoderFor(TestIdTokens.newRsaKeyPair()),
            Set.of("ios-client"),
            Set.of("edu.test.lostandfound"));
        SocialAuthService service = new SocialAuthService(verifier,
            new OAuth2AuthenticationService(users, jwtService, authUtil, 60_000L), authUtil, jwtService,
            new FakeAppleTokens(), users);

        mockMvc = MockMvcBuilders
            .standaloneSetup(new SocialAuthController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void googleLogin_rejectsAMissingToken() throws Exception {
        mockMvc.perform(post("/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("Sign-in token is required.")));
    }

    @Test
    void googleLogin_rejectsAnInvalidToken() throws Exception {
        mockMvc.perform(post("/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\": \"not-a-real-token\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void appleLogin_rejectsAnInvalidToken() throws Exception {
        mockMvc.perform(post("/auth/apple")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\": \"not-a-real-token\"}"))
            .andExpect(status().isUnauthorized());
    }
}
