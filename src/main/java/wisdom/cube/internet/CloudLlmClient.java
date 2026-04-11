package wisdom.cube.internet;

import java.util.Optional;

/**
 * Pluggable cloud completion (F5.T1.S1). Implementations perform HTTPS to the vendor API.
 */
public interface CloudLlmClient {

    /**
     * One completion request. Returns empty on transport/API failure (caller falls back to on-device).
     */
    Optional<String> complete(String sanitizedUserMessage);
}
