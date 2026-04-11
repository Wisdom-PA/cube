package wisdom.cube;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class CubeTest {

    @Test
    void cubeClassExists() {
        assertNotNull(Cube.class);
    }

    @Test
    void mainRunsWithoutErrorWhenNoPortConfigured() {
        Cube.main(new String[0]);
    }

    @Test
    void resolveListenPortFromDashDashPort() {
        assertEquals(8080, Cube.resolveListenPort(new String[] {"--port", "8080"}, null));
        assertEquals(0, Cube.resolveListenPort(new String[] {"--port", "0"}, null));
    }

    @Test
    void resolveListenPortFromEnvWhenNoArg() {
        assertEquals(9090, Cube.resolveListenPort(new String[0], "9090"));
        assertEquals(9090, Cube.resolveListenPort(new String[0], " 9090 "));
    }

    @Test
    void resolveListenPortArgOverridesEnv() {
        assertEquals(1111, Cube.resolveListenPort(new String[] {"--port", "1111"}, "2222"));
    }

    @Test
    void resolveListenPortReturnsNullWhenUnset() {
        assertNull(Cube.resolveListenPort(new String[0], null));
        assertNull(Cube.resolveListenPort(new String[] {"other"}, ""));
    }

    @Test
    void resolveListenPortRejectsInvalidArg() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            Cube.resolveListenPort(new String[] {"--port", "nope"}, null));
        assertTrue(assertInstanceOf(NumberFormatException.class, ex.getCause()).getMessage().contains("nope"));
    }

    @Test
    void resolveListenPortRejectsInvalidEnv() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            Cube.resolveListenPort(new String[0], "bad"));
        assertTrue(assertInstanceOf(NumberFormatException.class, ex.getCause()).getMessage().contains("bad"));
    }
}
