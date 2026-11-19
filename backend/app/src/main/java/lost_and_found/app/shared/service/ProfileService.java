package lost_and_found.app.shared.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lost_and_found.app.exceptions.UserExistsException;
import lost_and_found.app.exceptions.UserNotFoundException;
import lost_and_found.app.shared.domain.User;
import lost_and_found.app.shared.dto.UpdateProfileDTO;
import lost_and_found.app.shared.infra.UserRepository;

/** Edits the signed-in user's own editable profile fields (username and photo). */
@Service
public class ProfileService {

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Updates the user's username and photo. The username (already normalized to lowercase by the
     * DTO) must be unique; a clash throws {@link UserExistsException} (409). Passing a null photo
     * clears it.
     */
    @Transactional
    public User updateProfile(Long userId, UpdateProfileDTO dto) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User " + userId + " not found"));

        String newUsername = dto.username();
        boolean usernameChanged = !newUsername.equalsIgnoreCase(user.getUsername());
        if (usernameChanged && userRepository.existsByUsername(newUsername)) {
            throw new UserExistsException("That username is already taken.");
        }

        user.setUsername(newUsername);
        user.setPicture(dto.picture());
        return userRepository.save(user);
    }
}
