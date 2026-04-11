package wisdom.cube.vad;

/**
 * Utterance boundary detection (F3.T3.S3). Real VAD replaces this on device.
 */
public interface VoiceActivityDetector {

    /**
     * {@code true} when the current buffer likely ends an utterance.
     */
    boolean endOfUtteranceLikely();
}
