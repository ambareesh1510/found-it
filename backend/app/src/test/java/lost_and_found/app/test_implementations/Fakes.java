package lost_and_found.app.test_implementations;

import java.lang.reflect.Proxy;
import java.util.List;

import lost_and_found.app.searches.infra.LostSearchRepository;

/** Small test doubles for collaborators that a test does not need to exercise. */
public final class Fakes {

    private Fakes() {}

    /** A stand-in whose methods all fail loudly. Use it for collaborators a test never reaches. */
    public static <T> T unsupported(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
            (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "toString" -> type.getSimpleName() + " test double";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    };
                }
                throw new UnsupportedOperationException(
                    type.getSimpleName() + "." + method.getName() + " is not implemented by this test double.");
            }));
    }

    /** A LostSearchRepository that records which users' searches were deleted. */
    public static LostSearchRepository recordingLostSearches(List<Long> deletedForUserIds) {
        return (LostSearchRepository) Proxy.newProxyInstance(
            LostSearchRepository.class.getClassLoader(),
            new Class<?>[] { LostSearchRepository.class },
            (proxy, method, args) -> {
                if (method.getName().equals("deleteByUser_Id")) {
                    deletedForUserIds.add((Long) args[0]);
                    return null;
                }
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "toString" -> "LostSearchRepository test double";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    };
                }
                throw new UnsupportedOperationException(
                    "LostSearchRepository." + method.getName() + " is not implemented by this test double.");
            });
    }
}
