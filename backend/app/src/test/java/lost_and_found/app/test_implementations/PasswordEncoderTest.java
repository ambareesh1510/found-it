package lost_and_found.app.test_implementations;

import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordEncoderTest implements PasswordEncoder {

    @Override
    public @Nullable String encode(@Nullable CharSequence rawPassword) {
        return "ENC(" + rawPassword.toString() + ")";
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return encode(rawPassword).equals(encodedPassword);
    }
}
