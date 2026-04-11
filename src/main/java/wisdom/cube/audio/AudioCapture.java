package wisdom.cube.audio;

import java.util.Optional;

/**
 * Microphone capture path (F3.T1). Real hardware implementation replaces stubs on device.
 */
public interface AudioCapture extends AutoCloseable {

    /**
     * Next buffered chunk, or empty if none yet.
     */
    Optional<AudioChunk> readNext();

    @Override
    void close();
}
