package wisdom.cube.gateway;

/**
 * Structured error bodies for routine routes (F6.T5); matches contract {@code ApiError}.
 */
public final class RoutineApiErrors {

    private RoutineApiErrors() {
    }

    public static String routineNotFoundJson() {
        return "{\"error\":{\"code\":\"ROUTINE_NOT_FOUND\",\"message\":\"Unknown routine id\"}}";
    }

    public static String routinePatchUnsupportedJson() {
        return "{\"error\":{\"code\":\"ROUTINE_PATCH_UNSUPPORTED\","
            + "\"message\":\"This routine catalog does not support edits\"}}";
    }

    public static String routinePatchInvalidJson() {
        return "{\"error\":{\"code\":\"ROUTINE_PATCH_INVALID\",\"message\":\"Missing or empty name\"}}";
    }
}
