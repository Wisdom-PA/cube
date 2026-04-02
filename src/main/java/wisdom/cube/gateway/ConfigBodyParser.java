package wisdom.cube.gateway;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal JSON string handling for {@code DeviceConfig} PATCH bodies (no external JSON library).
 */
public final class ConfigBodyParser {

    static final Pattern DEVICE_NAME_FIELD = Pattern.compile(
        "\"device_name\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    static final Pattern PRIVACY_FIELD = Pattern.compile(
        "\"default_privacy_mode\"\\s*:\\s*\"(paranoid|normal)\"");

    private ConfigBodyParser() {
    }

    /**
     * Mutable device config fields updated by {@link #applyPatch(String, MutableConfig)}.
     */
    public static final class MutableConfig {
        /** Package-private for {@link #applyPatch}; use getters from gateway code. */
        String deviceName;
        String defaultPrivacyMode;

        public MutableConfig(String deviceName, String defaultPrivacyMode) {
            this.deviceName = deviceName;
            this.defaultPrivacyMode = defaultPrivacyMode;
        }

        public String getDeviceName() {
            return deviceName;
        }

        public String getDefaultPrivacyMode() {
            return defaultPrivacyMode;
        }
    }

    public static String jsonEscape(String raw) {
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

    public static String jsonUnescape(String escaped) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < escaped.length(); i++) {
            char c = escaped.charAt(i);
            if (c == '\\' && i + 1 < escaped.length()) {
                char n = escaped.charAt(i + 1);
                if (n == '\\' || n == '"') {
                    sb.append(n);
                    i++;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public static void applyPatch(String body, MutableConfig target) {
        if (body == null || body.isBlank()) {
            return;
        }
        Matcher nameMatcher = DEVICE_NAME_FIELD.matcher(body);
        if (nameMatcher.find()) {
            target.deviceName = jsonUnescape(nameMatcher.group(1));
        }
        Matcher privacyMatcher = PRIVACY_FIELD.matcher(body);
        if (privacyMatcher.find()) {
            target.defaultPrivacyMode = privacyMatcher.group(1);
        }
    }
}
