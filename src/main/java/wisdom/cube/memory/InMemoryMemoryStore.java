package wisdom.cube.memory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory only; suitable for tests and early bring-up.
 */
public final class InMemoryMemoryStore implements MemoryStore {

    private final Map<String, String> backing = new ConcurrentHashMap<>();

    private static String compoundKey(String profileId, String key) {
        return profileId + "\u0000" + key;
    }

    @Override
    public void remember(String profileId, String key, String value) {
        backing.put(compoundKey(profileId, key), value);
    }

    @Override
    public Optional<String> recall(String profileId, String key) {
        return Optional.ofNullable(backing.get(compoundKey(profileId, key)));
    }

    @Override
    public void forget(String profileId, String key) {
        backing.remove(compoundKey(profileId, key));
    }

    @Override
    public void forgetProfile(String profileId) {
        String prefix = profileId + "\u0000";
        backing.keySet().removeIf(k -> k.startsWith(prefix));
    }

    @Override
    public void forgetKeyPrefix(String profileId, String keyPrefix) {
        if (keyPrefix == null) {
            return;
        }
        String start = compoundKey(profileId, keyPrefix);
        backing.keySet().removeIf(k -> k.startsWith(start));
    }

    @Override
    public Map<String, String> exportProfile(String profileId) {
        String prefix = profileId + "\u0000";
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : backing.entrySet()) {
            String k = e.getKey();
            if (k.startsWith(prefix)) {
                out.put(k.substring(prefix.length()), e.getValue());
            }
        }
        return Map.copyOf(out);
    }
}
