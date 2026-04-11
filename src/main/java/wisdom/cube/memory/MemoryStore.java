package wisdom.cube.memory;

import java.time.LocalDate;
import java.util.Map;
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

    /**
     * Keys stored under {@code mem/<day>/…} for “forget today” (F4.T4.S3).
     */
    default String memoryDayPrefix(LocalDate day) {
        return "mem/" + day + "/";
    }

    default void rememberForDay(String profileId, LocalDate day, String subKey, String value) {
        remember(profileId, memoryDayPrefix(day) + subKey, value);
    }

    default void forgetDay(String profileId, LocalDate day) {
        forgetKeyPrefix(profileId, memoryDayPrefix(day));
    }

    /**
     * All logical keys and values for the profile (F4.T4.S4).
     */
    Map<String, String> exportProfile(String profileId);
}
