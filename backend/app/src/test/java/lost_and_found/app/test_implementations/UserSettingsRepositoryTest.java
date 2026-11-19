package lost_and_found.app.test_implementations;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import lost_and_found.app.shared.domain.UserSettings;
import lost_and_found.app.shared.infra.UserSettingsRepository;

/**
 * In-memory {@link UserSettingsRepository} double. Implements only the methods the settings
 * service uses ({@code save}, {@code findById}, {@code existsById}, {@code deleteById}); anything
 * else fails loudly so a test cannot silently rely on unimplemented behavior.
 */
public final class UserSettingsRepositoryTest {

    private UserSettingsRepositoryTest() {}

    public static UserSettingsRepository inMemory() {
        Map<Long, UserSettings> byId = new HashMap<>();
        return (UserSettingsRepository) Proxy.newProxyInstance(
            UserSettingsRepository.class.getClassLoader(),
            new Class<?>[] { UserSettingsRepository.class },
            (proxy, method, args) -> switch (method.getName()) {
                case "save", "saveAndFlush" -> {
                    UserSettings s = (UserSettings) args[0];
                    // Mirror @MapsId: a new row has a null id derived from its user on persist.
                    if (s.getUserId() == null && s.getUser() != null) {
                        s.setUserId(s.getUser().getId());
                    }
                    byId.put(s.getUserId(), s);
                    yield s;
                }
                case "findById" -> Optional.ofNullable(byId.get((Long) args[0]));
                case "existsById" -> byId.containsKey((Long) args[0]);
                case "deleteById" -> {
                    byId.remove((Long) args[0]);
                    yield null;
                }
                case "toString" -> "UserSettingsRepository test double";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(
                    "UserSettingsRepository." + method.getName() + " is not implemented by this test double.");
            });
    }
}
