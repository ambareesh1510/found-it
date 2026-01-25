package lost_and_found.app.shared.infra;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lost_and_found.app.exceptions.UserNotFoundException;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.dto.CurrentUserResponseDTO;
import lost_and_found.app.shared.dto.UserProfileResponseDTO;
import lost_and_found.app.shared.service.AccountService;
import lost_and_found.app.shared.util.RequestTokens;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final AccountService accountService;

    public UserController(UserRepository userRepository, AccountService accountService) {
        this.userRepository = userRepository;
        this.accountService = accountService;
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
    public ResponseEntity<UserProfileResponseDTO> getUserById(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User " + userId + " not found"));

        return ResponseEntity.ok(toUserProfileResponse(user));
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

    private UserProfileResponseDTO toUserProfileResponse(User user) {
        return new UserProfileResponseDTO(
            user.getId(),
            user.getUsername()
        );
    }
}
