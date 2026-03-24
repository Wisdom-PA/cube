package wisdom.cube.core;

import java.util.Optional;

/**
 * Speech-to-text: turns audio into transcribed text.
 * Mocked in unit tests; real implementation in Phase 6.
 */
public interface SttService {

    /**
     * Transcribe the next utterance from the stream (or buffered audio).
     * Returns empty when no speech detected or stream ended.
     */
    Optional<String> transcribe();
}
