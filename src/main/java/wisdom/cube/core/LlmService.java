package wisdom.cube.core;

import java.util.Optional;

/**
 * On-device (or optionally cloud) LLM: prompt in, response out.
 * Mocked in unit tests; real implementation in Phase 6.
 */
public interface LlmService {

    /**
     * Complete one turn: send prompt, return model response text.
     * Returns empty on failure or timeout.
     */
    Optional<String> complete(String prompt);
}
