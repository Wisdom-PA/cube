package wisdom.cube.vad;

/** Always signals end-of-utterance (pipeline tests). */
public final class StubVoiceActivityDetector implements VoiceActivityDetector {

    @Override
    public boolean endOfUtteranceLikely() {
        return true;
    }
}
