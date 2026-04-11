package wisdom.cube.audio;

import java.util.Arrays;

/**
 * Immutable snapshot of PCM (or encoded) audio for capture/playback (F3.T1).
 */
public final class AudioChunk {

    private final byte[] data;
    private final int sampleRateHz;
    private final boolean stereo;

    public AudioChunk(byte[] data, int sampleRateHz, boolean stereo) {
        this.data = Arrays.copyOf(data, data.length);
        this.sampleRateHz = sampleRateHz;
        this.stereo = stereo;
    }

    public byte[] data() {
        return Arrays.copyOf(data, data.length);
    }

    public int sampleRateHz() {
        return sampleRateHz;
    }

    public boolean stereo() {
        return stereo;
    }

    public int byteLength() {
        return data.length;
    }
}
