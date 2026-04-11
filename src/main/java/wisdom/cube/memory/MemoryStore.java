package wisdom.cube.memory;

import java.util.Optional;

/**
 * Minimal per-profile memory (F4.T4.S1–S3 scaffold). Encrypted storage is a later task.
 */
public interface MemoryStore {

    void remember(String profileId, String key, String value);

    Optional<String> recall(String profileId, String key);

    void forget(String profileId, String key);

    void forgetProfile(String profileId);

    /**
     * Removes all keys for the profile whose logical key starts with {@code keyPrefix} (F4.T4.S3 bulk forget).
     */
    void forgetKeyPrefix(String profileId, String keyPrefix);
}
