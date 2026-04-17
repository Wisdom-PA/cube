package wisdom.cube.routine;

/**
 * Minimal JSON string escaping for routine summaries (no extra dependencies).
 */
public final class RoutineJson {

    private RoutineJson() {
    }

    public static String escape(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return raw
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
