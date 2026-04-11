package wisdom.cube.audio;

import wisdom.cube.core.SttService;

import java.util.Optional;

/**
 * Drains {@link AudioCapture} through {@link AudioPreprocessor}, discarding PCM (F3.T5), then returns
 * a fixed transcript — for tests and host demos without a real STT runtime (F3.T3.S2 scaffold).
 */
public final class AudioAwareStubSttService implements SttService {

    private final AudioCapture capture;
    private final AudioPreprocessor preprocessor;
    private final String transcript;

    public AudioAwareStubSttService(
        AudioCapture capture,
        AudioPreprocessor preprocessor,
        String transcript
    ) {
        this.capture = capture;
        this.preprocessor = preprocessor;
        this.transcript = transcript;
    }

    @Override
    public Optional<String> transcribe() {
        Optional<AudioChunk> next;
        while ((next = capture.readNext()).isPresent()) {
            AudioChunk raw = next.get();
            AudioChunk processed = preprocessor.process(raw);
            if (processed != raw) {
                raw.discardPayload();
            }
            processed.discardPayload();
        }
        if (transcript == null || transcript.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(transcript);
    }
}
