package wisdom.cube.core;

/**
 * Text-to-speech: synthesizes speech from text.
 * Mocked in unit tests; real implementation in Phase 6.
 */
public interface TtsService {

    /**
     * Speak the given text. Blocks until playback completes or fails.
     */
    void speak(String text);
}
