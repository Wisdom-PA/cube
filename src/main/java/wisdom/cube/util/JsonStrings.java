package wisdom.cube.util;

/**
 * Minimal JSON string escaping (no external JSON library).
 */
public final class JsonStrings {

    private JsonStrings() {
    }

    /**
     * Escapes a string for use inside JSON double-quoted string values.
     */
    public static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\\' || c == '"') {
                sb.append('\\');
            }
            if (c == '\n' || c == '\r' || c == '\t') {
                sb.append(' ');
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
