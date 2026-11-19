package lost_and_found.app.shared.infra;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lost_and_found.app.exceptions.UserNotFoundException;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.dto.CurrentUserResponseDTO;
import lost_and_found.app.shared.dto.UpdateProfileDTO;
import lost_and_found.app.shared.dto.UserProfileResponseDTO;
import lost_and_found.app.shared.dto.UserSettingsDTO;
import lost_and_found.app.shared.service.AccountService;
import lost_and_found.app.shared.service.ProfileService;
import lost_and_found.app.shared.service.SettingsService;
import lost_and_found.app.shared.util.RequestTokens;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final AccountService accountService;
    private final ProfileService profileService;
    private final SettingsService settingsService;

    public UserController(
        UserRepository userRepository,
        AccountService accountService,
        ProfileService profileService,
        SettingsService settingsService
    ) {
        this.userRepository = userRepository;
        this.accountService = accountService;
        this.profileService = profileService;
        this.settingsService = settingsService;
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponseDTO> getCurrentUser(
        @AuthenticationPrincipal User user
    ) {
        if (user == null) throw new UserNotFoundException("User Not Authenticated");

        User persistedUser = userRepository.findById(user.getId())
            .orElseThrow(() -> new UserNotFoundException("User " + user.getId() + " not found"));

        return ResponseEntity.ok(toCurrentUserResponse(persistedUser));
    }

    /** Updates the signed-in user's editable profile fields (username and photo). */
    @PutMapping("/me")
    public ResponseEntity<CurrentUserResponseDTO> updateCurrentUser(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody UpdateProfileDTO body
    ) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        User updated = profileService.updateProfile(user.getId(), body);
        return ResponseEntity.ok(toCurrentUserResponse(updated));
    }

    /** The signed-in user's Settings toggles (created with defaults the first time). */
    @GetMapping("/me/settings")
    public ResponseEntity<UserSettingsDTO> getMySettings(
        @AuthenticationPrincipal User user
    ) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(UserSettingsDTO.from(settingsService.getOrCreateForUser(user.getId())));
    }

    /** Replaces the signed-in user's Settings toggles. */
    @PutMapping("/me/settings")
    public ResponseEntity<UserSettingsDTO> updateMySettings(
        @AuthenticationPrincipal User user,
        @RequestBody UserSettingsDTO body
    ) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(UserSettingsDTO.from(settingsService.update(user.getId(), body)));
    }

    /** Deletes the signed-in user's account and personal data. See {@link AccountService#deleteAccount}. */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser(
        @AuthenticationPrincipal User user,
        HttpServletRequest request
    ) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        accountService.deleteAccount(user.getId(), RequestTokens.resolve(request));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponseDTO> getUserById(
        @AuthenticationPrincipal User requester,
        @PathVariable Long userId
    ) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User " + userId + " not found"));

        // A private profile hides its name from everyone except the user themselves.
        boolean isSelf = requester != null && requester.getId().equals(userId);
        boolean hideName = !isSelf && settingsService.isPrivate(userId);
        return ResponseEntity.ok(toUserProfileResponse(user, hideName));
    }

    private CurrentUserResponseDTO toCurrentUserResponse(User user) {
        return new CurrentUserResponseDTO(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.isEnabled(),
            user.getPicture(),
            user.getAuthProviderType(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    private UserProfileResponseDTO toUserProfileResponse(User user, boolean hideName) {
        return new UserProfileResponseDTO(
            user.getId(),
            hideName ? null : user.getUsername()
        );
    }
}
