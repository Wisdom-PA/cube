package wisdom.cube.audio;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioChunkAndBufferTest {

    @Test
    void audioChunkCopiesData() {
        byte[] raw = {1, 2, 3};
        AudioChunk c = new AudioChunk(raw, 16000, false);
        raw[0] = 9;
        assertArrayEquals(new byte[] {1, 2, 3}, c.data());
        assertEquals(16000, c.sampleRateHz());
        assertFalse(c.stereo());
        assertEquals(3, c.byteLength());
    }

    @Test
    void ringBufferDrainsAndClearsOnClose() {
        try (InMemoryAudioRingBuffer buf = new InMemoryAudioRingBuffer()) {
            buf.push(new AudioChunk(new byte[] {5}, 8000, false));
            assertTrue(buf.readNext().isPresent());
            assertFalse(buf.readNext().isPresent());
        }
    }

    @Test
    void ringBufferNoReadsAfterClose() {
        InMemoryAudioRingBuffer buf = new InMemoryAudioRingBuffer();
        buf.push(new AudioChunk(new byte[] {1}, 8000, false));
        buf.close();
        assertEquals(Optional.empty(), buf.readNext());
    }

    @Test
    void zeroOutClearsArray() {
        byte[] b = {1, 2, 3};
        InMemoryAudioRingBuffer.zeroOut(b);
        assertArrayEquals(new byte[] {0, 0, 0}, b);
    }

    @Test
    void discardPayloadZerosAndBlocksData() {
        AudioChunk c = new AudioChunk(new byte[] {7, 8}, 16000, false);
        assertArrayEquals(new byte[] {7, 8}, c.data());
        c.discardPayload();
        assertEquals(0, c.byteLength());
        assertFalse(c.hasPayload());
        assertThrows(IllegalStateException.class, c::data);
    }

    @Test
    void closeDiscardsQueuedChunks() {
        InMemoryAudioRingBuffer buf = new InMemoryAudioRingBuffer();
        AudioChunk pending = new AudioChunk(new byte[] {3, 3, 3}, 8000, false);
        buf.push(pending);
        buf.close();
        assertFalse(pending.hasPayload());
    }
}
