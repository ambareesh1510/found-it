package lost_and_found.app.test_implementations;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import lost_and_found.app.shared.infra.UserRepository;

public class UserDetailsServiceTest implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceTest(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsernameOrEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
