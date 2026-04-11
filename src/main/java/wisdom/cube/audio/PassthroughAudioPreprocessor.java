package wisdom.cube.audio;

/**
 * No-op preprocessor (F3.T1.S2/S3 placeholder until native beamforming/AEC is wired).
 */
public final class PassthroughAudioPreprocessor implements AudioPreprocessor {

    public static final PassthroughAudioPreprocessor INSTANCE = new PassthroughAudioPreprocessor();

    private PassthroughAudioPreprocessor() {
    }

    @Override
    public AudioChunk process(AudioChunk input) {
        return input;
    }
}
