package lost_and_found.app.test_implementations;

import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthenticationManagerTest implements AuthenticationManager {

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (authentication == null) {
            throw new BadCredentialsException("No authentication request provided");
        }

        String username = authentication.getName();
        Object credentialsObj = authentication.getCredentials();
        String password = credentialsObj != null ? credentialsObj.toString() : null;

        // Simple test behavior (customize as needed)
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BadCredentialsException("Invalid username or password");
        }

        // Optional hardcoded check for testing:
        // if (!username.equals("testuser") || !password.equals("testpass")) {
        //     throw new BadCredentialsException("Bad credentials");
        // }

        UserDetails userDetails = User.withUsername(username)
            .password(password) // only for testing; don't do this in real auth
            .authorities(List.of())
            .build();

        return new UsernamePasswordAuthenticationToken(
            userDetails,       // principal
            null,              // erase credentials in returned token
            userDetails.getAuthorities()
        );
    }
}