package wisdom.cube.wakeword;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StubWakeWordDetectorTest {

    @Test
    void firesAfterConfiguredPolls() {
        StubWakeWordDetector d = new StubWakeWordDetector(2);
        assertFalse(d.poll());
        assertFalse(d.poll());
        assertTrue(d.poll());
        assertFalse(d.poll());
    }

    @Test
    void reset() {
        StubWakeWordDetector d = new StubWakeWordDetector(0);
        assertTrue(d.poll());
        d.reset(1);
        assertFalse(d.poll());
        assertTrue(d.poll());
    }

    @Test
    void rejectsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new StubWakeWordDetector(-1));
    }
}
