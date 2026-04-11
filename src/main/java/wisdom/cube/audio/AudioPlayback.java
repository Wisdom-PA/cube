package wisdom.cube.audio;

/**
 * Speaker output path (F3.T1).
 */
public interface AudioPlayback {

    void play(AudioChunk chunk);

    /**
     * Linear gain 0.0–1.0 (best effort on real hardware).
     */
    void setVolume(float linearGain);
}
