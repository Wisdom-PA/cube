package wisdom.cube.supervisor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceRestartPolicyTest {

    @Test
    void rejectsNegativeMax() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> new ServiceRestartPolicy(-1, 0L));
        assertEquals("maxRestarts must be >= 0", ex.getMessage());
    }

    @Test
    void noneHasZeroRestarts() {
        assertEquals(0, ServiceRestartPolicy.none().maxRestarts());
    }
}
