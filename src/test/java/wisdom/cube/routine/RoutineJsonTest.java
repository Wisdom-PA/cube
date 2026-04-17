package wisdom.cube.routine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoutineJsonTest {

    @Test
    void escapeQuotesAndBackslashes() {
        assertEquals("", RoutineJson.escape(""));
        assertEquals("", RoutineJson.escape(null));
        assertEquals("a\\\\b", RoutineJson.escape("a\\b"));
        assertEquals("say \\\"hi\\\"", RoutineJson.escape("say \"hi\""));
    }
}
