package wisdom.cube.audio;

/**
 * No-op playback for tests and headless CI (F3.T4 stub until Piper is integrated).
 * Does not retain {@link AudioChunk} bytes; callers may {@link AudioChunk#discardPayload()} after play.
 */
public final class SilentAudioPlayback implements AudioPlayback {

    private float volume = 1.0f;

    @Override
    public void play(AudioChunk chunk) {
        // intentionally empty — no audio device
    }

    @Override
    public void setVolume(float linearGain) {
        this.volume = Math.max(0f, Math.min(1f, linearGain));
    }

    public float getVolume() {
        return volume;
    }
}
