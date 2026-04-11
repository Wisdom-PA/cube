package wisdom.cube.vad;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StubVoiceActivityDetectorTest {

    @Test
    void alwaysTrue() {
        assertTrue(new StubVoiceActivityDetector().endOfUtteranceLikely());
    }
}
