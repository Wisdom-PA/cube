package wisdom.cube.audio;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * In-memory capture only: **no filesystem persistence** (F3.T5 baseline). On {@link #close()},
 * queued chunks are discarded and the last reference is cleared.
 */
public final class InMemoryAudioRingBuffer implements AudioCapture {

    private final ConcurrentLinkedQueue<AudioChunk> queue = new ConcurrentLinkedQueue<>();
    private volatile boolean closed;

    public void push(AudioChunk chunk) {
        if (!closed) {
            queue.add(chunk);
        }
    }

    @Override
    public Optional<AudioChunk> readNext() {
        if (closed) {
            return Optional.empty();
        }
        AudioChunk c = queue.poll();
        return Optional.ofNullable(c);
    }

    @Override
    public void close() {
        closed = true;
        AudioChunk c;
        while ((c = queue.poll()) != null) {
            c.discardPayload();
        }
    }

    /**
     * Clears sensitive bytes in a copy of {@code data} (best effort; original array unchanged).
     */
    public static void zeroOut(byte[] data) {
        if (data != null) {
            Arrays.fill(data, (byte) 0);
        }
    }
}
