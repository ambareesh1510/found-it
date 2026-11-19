package lost_and_found.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lost_and_found.app.exceptions.UserExistsException;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.dto.UpdateProfileDTO;
import lost_and_found.app.shared.service.ProfileService;
import lost_and_found.app.test_implementations.UserRepositoryTest;

class ProfileServiceTest {

    private UserRepositoryTest users;
    private ProfileService profileService;
    private User alice;

    @BeforeEach
    void setUp() {
        users = new UserRepositoryTest();
        profileService = new ProfileService(users);
        alice = users.save(User.builder().username("alice").email("alice@test.com").enabled(true)
            .picture("old.png").build());
    }

    @Test
    void updatesUsernameAndPicture() {
        User updated = profileService.updateProfile(alice.getId(), new UpdateProfileDTO("alice2", "new.png"));

        assertEquals("alice2", updated.getUsername());
        assertEquals("new.png", updated.getPicture());
    }

    @Test
    void blankPictureClearsThePhoto() {
        User updated = profileService.updateProfile(alice.getId(), new UpdateProfileDTO("alice", "   "));

        assertNull(updated.getPicture());
    }

    @Test
    void rejectsAUsernameAlreadyTaken() {
        users.save(User.builder().username("bob").email("bob@test.com").enabled(true).build());

        assertThrows(UserExistsException.class,
            () -> profileService.updateProfile(alice.getId(), new UpdateProfileDTO("bob", null)));
    }

    @Test
    void keepingTheSameUsernameIsAllowed() {
        User updated = profileService.updateProfile(alice.getId(), new UpdateProfileDTO("Alice", "p.png"));

        assertEquals("alice", updated.getUsername(), "DTO lowercases, and an unchanged name is not a clash");
    }
}
