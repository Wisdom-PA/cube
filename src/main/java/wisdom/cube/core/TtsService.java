package wisdom.cube.core;

/**
 * Text-to-speech: synthesizes speech from text.
 * Mocked in unit tests; Piper or device audio plugs in behind {@code wisdom.cube.voice.VoiceTurnPipeline}.
 */
public interface TtsService {

    /**
     * Speak the given text. Blocks until playback completes or fails.
     */
    void speak(String text);
}
