package wisdom.cube.wakeword;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

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
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> new StubWakeWordDetector(-1));
        assertEquals("pollsBeforeFire must be >= 0", ex.getMessage());
    }
}
