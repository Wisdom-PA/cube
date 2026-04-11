package wisdom.cube.audio;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AudioAwareStubSttServiceTest {

    @Test
    void transcribeDrainsChunksAndDiscardsPayloadPassthrough() {
        try (InMemoryAudioRingBuffer buf = new InMemoryAudioRingBuffer()) {
            AudioChunk c = new AudioChunk(new byte[] {1, 2, 3}, 16000, false);
            buf.push(c);
            AudioAwareStubSttService stt = new AudioAwareStubSttService(
                buf,
                PassthroughAudioPreprocessor.INSTANCE,
                "hello"
            );
            assertEquals(Optional.of("hello"), stt.transcribe());
            assertFalse(c.hasPayload());
        }
    }

    @Test
    void transcribeReturnsEmptyWhenTranscriptBlank() {
        try (InMemoryAudioRingBuffer buf = new InMemoryAudioRingBuffer()) {
            buf.push(new AudioChunk(new byte[] {1}, 8000, false));
            AudioAwareStubSttService stt = new AudioAwareStubSttService(
                buf,
                PassthroughAudioPreprocessor.INSTANCE,
                "   "
            );
            assertEquals(Optional.empty(), stt.transcribe());
        }
    }

    @Test
    void transcribeDiscardsBothWhenPreprocessorReturnsNewChunk() {
        try (InMemoryAudioRingBuffer buf = new InMemoryAudioRingBuffer()) {
            AudioChunk original = new AudioChunk(new byte[] {9}, 48000, false);
            buf.push(original);
            AudioPreprocessor pre = in -> new AudioChunk(in.data(), in.sampleRateHz(), in.stereo());
            AudioAwareStubSttService stt = new AudioAwareStubSttService(buf, pre, "x");
            assertEquals(Optional.of("x"), stt.transcribe());
            assertFalse(original.hasPayload());
        }
    }

    @Test
    void audioPrivacySettingsDefaultDisallowsPersistence() {
        assertFalse(AudioPrivacySettings.productionDefault().allowRawPersistence());
    }
}
