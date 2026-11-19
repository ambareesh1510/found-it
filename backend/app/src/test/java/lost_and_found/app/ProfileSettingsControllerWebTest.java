package lost_and_found.app;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import lost_and_found.app.auth.service.BlacklistService;
import lost_and_found.app.auth.service.HS256JwtService;
import lost_and_found.app.auth.service.JwtService;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.infra.GlobalExceptionHandler;
import lost_and_found.app.shared.infra.UserController;
import lost_and_found.app.shared.service.AccountService;
import lost_and_found.app.shared.service.ProfileService;
import lost_and_found.app.shared.service.SettingsService;
import lost_and_found.app.test_implementations.BlacklistServiceTest;
import lost_and_found.app.test_implementations.FakeAppleTokens;
import lost_and_found.app.test_implementations.Fakes;
import lost_and_found.app.test_implementations.UserRepositoryTest;
import lost_and_found.app.test_implementations.UserSettingsRepositoryTest;

class ProfileSettingsControllerWebTest {

    private UserRepositoryTest users;
    private MockMvc mockMvc;
    private User alice;

    @BeforeEach
    void setUp() {
        String secret = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
        users = new UserRepositoryTest();
        BlacklistService blacklist = new BlacklistServiceTest();
        JwtService jwtService = new HS256JwtService(blacklist, secret, 60_000L);
        AccountService accountService = new AccountService(users,
            Fakes.recordingLostSearches(new ArrayList<>()), blacklist, jwtService, new FakeAppleTokens());
        ProfileService profileService = new ProfileService(users);
        SettingsService settingsService = new SettingsService(UserSettingsRepositoryTest.inMemory(), users);

        mockMvc = MockMvcBuilders
            .standaloneSetup(new UserController(users, accountService, profileService, settingsService))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        alice = users.save(User.builder().username("alice").email("alice@test.com").enabled(true).build());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void signIn(User user) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    // --- profile update ---

    @Test
    void putMe_requiresAuthentication() throws Exception {
        mockMvc.perform(put("/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newname\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void putMe_updatesUsernameAndPhoto() throws Exception {
        signIn(alice);

        mockMvc.perform(put("/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"NewName\",\"picture\":\"pic.png\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("newname"))
            .andExpect(jsonPath("$.picture").value("pic.png"));
    }

    @Test
    void putMe_rejectsBlankUsername() throws Exception {
        signIn(alice);

        mockMvc.perform(put("/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"   \"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void putMe_rejectsDuplicateUsername() throws Exception {
        users.save(User.builder().username("bob").email("bob@test.com").enabled(true).build());
        signIn(alice);

        mockMvc.perform(put("/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"bob\"}"))
            .andExpect(status().isConflict());
    }

    // --- settings ---

    @Test
    void getSettings_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/users/me/settings"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getSettings_returnsPrototypeDefaults() throws Exception {
        signIn(alice);

        mockMvc.perform(get("/users/me/settings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationAccess").value(true))
            .andExpect(jsonPath("$.privateProfile").value(false))
            .andExpect(jsonPath("$.emailUpdates").value(false));
    }

    @Test
    void putSettings_persistsChanges() throws Exception {
        signIn(alice);

        mockMvc.perform(put("/users/me/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"locationAccess\":false,\"privateProfile\":true,\"pushNotifications\":true,"
                    + "\"lostItemAlerts\":true,\"foundItemMatches\":true,\"notificationSound\":true,"
                    + "\"emailUpdates\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.privateProfile").value(true));

        mockMvc.perform(get("/users/me/settings"))
            .andExpect(jsonPath("$.privateProfile").value(true))
            .andExpect(jsonPath("$.locationAccess").value(false));
    }

    // --- private profile on public lookup ---

    @Test
    void getUserById_hidesNameOfPrivateProfileFromOthers() throws Exception {
        signIn(alice);
        mockMvc.perform(put("/users/me/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"locationAccess\":true,\"privateProfile\":true,\"pushNotifications\":true,"
                    + "\"lostItemAlerts\":true,\"foundItemMatches\":true,\"notificationSound\":true,"
                    + "\"emailUpdates\":false}"))
            .andExpect(status().isOk());

        User bob = users.save(User.builder().username("bob").email("bob@test.com").enabled(true).build());
        signIn(bob);

        mockMvc.perform(get("/users/" + alice.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value(nullValue()));
    }

    @Test
    void getUserById_showsOwnNameEvenWhenPrivate() throws Exception {
        signIn(alice);
        mockMvc.perform(put("/users/me/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"locationAccess\":true,\"privateProfile\":true,\"pushNotifications\":true,"
                    + "\"lostItemAlerts\":true,\"foundItemMatches\":true,\"notificationSound\":true,"
                    + "\"emailUpdates\":false}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/users/" + alice.getId()))
            .andExpect(jsonPath("$.username").value("alice"));
    }
}
