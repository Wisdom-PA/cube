package wisdom.cube.audio;

import java.util.Arrays;

/**
 * PCM (or encoded) snapshot for capture/playback (F3.T1). Internal payload can be
 * {@link #discardPayload()} to satisfy F3.T5 (zero-filled, then dropped).
 */
public final class AudioChunk {

    private final Object payloadLock = new Object();
    private byte[] data;
    private final int sampleRateHz;
    private final boolean stereo;

    public AudioChunk(byte[] data, int sampleRateHz, boolean stereo) {
        this.data = Arrays.copyOf(data, data.length);
        this.sampleRateHz = sampleRateHz;
        this.stereo = stereo;
    }

    /**
     * Defensive copy of PCM bytes; throws if payload was already discarded.
     */
    public byte[] data() {
        synchronized (payloadLock) {
            if (data == null) {
                throw new IllegalStateException("audio payload discarded");
            }
            return Arrays.copyOf(data, data.length);
        }
    }

    public int sampleRateHz() {
        return sampleRateHz;
    }

    public boolean stereo() {
        return stereo;
    }

    public int byteLength() {
        synchronized (payloadLock) {
            return data == null ? 0 : data.length;
        }
    }

    /**
     * Zero-fill and release PCM bytes (F3.T5.S2). Safe to call multiple times.
     */
    public void discardPayload() {
        synchronized (payloadLock) {
            if (data != null) {
                Arrays.fill(data, (byte) 0);
                data = null;
            }
        }
    }

    public boolean hasPayload() {
        synchronized (payloadLock) {
            return data != null;
        }
    }
}
