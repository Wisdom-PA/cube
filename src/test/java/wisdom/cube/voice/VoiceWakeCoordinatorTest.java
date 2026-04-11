package wisdom.cube.voice;

import org.junit.jupiter.api.Test;
import wisdom.cube.wakeword.StubWakeWordDetector;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceWakeCoordinatorTest {

    @Test
    void pollsUntilWake() {
        StubWakeWordDetector w = new StubWakeWordDetector(1);
        assertTrue(VoiceWakeCoordinator.pollUntilWake(w, 10));
    }

    @Test
    void givesUp() {
        StubWakeWordDetector w = new StubWakeWordDetector(100);
        assertFalse(VoiceWakeCoordinator.pollUntilWake(w, 3));
    }
}
