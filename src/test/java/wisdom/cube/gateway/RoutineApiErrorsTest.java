package wisdom.cube.gateway;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RoutineApiErrorsTest {

    @Test
    void errorJsonBodiesContainCodes() {
        assertTrue(RoutineApiErrors.routineNotFoundJson().contains("ROUTINE_NOT_FOUND"));
        assertTrue(RoutineApiErrors.routinePatchUnsupportedJson().contains("ROUTINE_PATCH_UNSUPPORTED"));
        assertTrue(RoutineApiErrors.routinePatchInvalidJson().contains("ROUTINE_PATCH_INVALID"));
    }
}
