package wisdom.cube.gateway;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * In-memory device list and PATCH parsing for the skeleton gateway (contract {@code PATCH /devices/{id}}).
 */
public final class DeviceFixtureStore {

    private static final Pattern POWER = Pattern.compile("\"power\"\\s*:\\s*(true|false)");
    private static final Pattern BRIGHTNESS = Pattern.compile("\"brightness\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");

    public static final class Entry {
        final String id;
        final String name;
        final String type;
        final String room;
        boolean power;
        double brightness;

        Entry(String id, String name, String type, String room, boolean power, double brightness) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.room = room;
            this.power = power;
            this.brightness = brightness;
        }
    }

    private final Map<String, Entry> byId = new LinkedHashMap<>();

    public DeviceFixtureStore() {
        byId.put("light-1", new Entry("light-1", "Living room light", "light", "Living room", true, 1.0));
        byId.put("light-2", new Entry("light-2", "Kitchen light", "light", "Kitchen", false, 1.0));
    }

    public String listJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"devices\":[");
        boolean first = true;
        for (Entry e : byId.values()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(deviceJson(e));
        }
        sb.append("]}");
        return sb.toString();
    }

    /**
     * @return updated device JSON object (no braces extra), or {@code null} if id unknown
     */
    public String patch(String deviceId, String body) {
        Entry e = byId.get(deviceId);
        if (e == null) {
            return null;
        }
        if (body != null && !body.isBlank()) {
            Matcher pm = POWER.matcher(body);
            if (pm.find()) {
                e.power = Boolean.parseBoolean(pm.group(1));
            }
            Matcher bm = BRIGHTNESS.matcher(body);
            if (bm.find()) {
                double v = Double.parseDouble(bm.group(1));
                e.brightness = Math.max(0.0, Math.min(1.0, v));
            }
        }
        return deviceJson(e);
    }

    private static String deviceJson(Entry e) {
        return "{"
            + "\"id\":\"" + ConfigBodyParser.jsonEscape(e.id) + "\","
            + "\"name\":\"" + ConfigBodyParser.jsonEscape(e.name) + "\","
            + "\"type\":\"" + ConfigBodyParser.jsonEscape(e.type) + "\","
            + "\"room\":\"" + ConfigBodyParser.jsonEscape(e.room) + "\","
            + "\"power\":" + e.power + ","
            + "\"brightness\":" + formatBrightness(e.brightness)
            + "}";
    }

    private static String formatBrightness(double b) {
        if (b == (long) b) {
            return String.valueOf((long) b);
        }
        return String.valueOf(b);
    }
}
