package wisdom.cube.audio;

/**
 * Transform captured audio before STT (F3.T1.S2 beamforming / noise reduction, F3.T1.S3 AEC).
 * Default implementation is a passthrough until native processing is integrated.
 */
public interface AudioPreprocessor {

    /**
     * Returns processed audio. If the result is a new {@link AudioChunk}, the caller must
     * {@link AudioChunk#discardPayload()} on both {@code input} and the returned chunk when done.
     */
    AudioChunk process(AudioChunk input);
}
