package wisdom.cube.gateway;

import wisdom.cube.device.InMemoryLightDeviceRegistry;
import wisdom.cube.device.LightDevice;
import wisdom.cube.device.LightDeviceRegistry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON list and PATCH parsing for the skeleton gateway (contract {@code PATCH /devices/{id}}),
 * backed by a shared {@link LightDeviceRegistry}.
 */
public final class DeviceFixtureStore {

    private static final Pattern POWER = Pattern.compile("\"power\"\\s*:\\s*(true|false)");
    private static final Pattern BRIGHTNESS = Pattern.compile(
        "\"brightness\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)");

    private final LightDeviceRegistry registry;

    public DeviceFixtureStore() {
        this(new InMemoryLightDeviceRegistry());
    }

    public DeviceFixtureStore(LightDeviceRegistry registry) {
        this.registry = registry;
    }

    public LightDeviceRegistry registry() {
        return registry;
    }

    public String listJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"devices\":[");
        boolean first = true;
        for (LightDevice e : registry.allInOrder()) {
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
        if (!registry.contains(deviceId)) {
            return null;
        }
        if (body != null && !body.isBlank()) {
            Matcher pm = POWER.matcher(body);
            if (pm.find()) {
                registry.setPower(deviceId, Boolean.parseBoolean(pm.group(1)));
            }
            Matcher bm = BRIGHTNESS.matcher(body);
            if (bm.find()) {
                double v = Double.parseDouble(bm.group(1));
                registry.setBrightness(deviceId, v);
            }
        }
        return registry.get(deviceId).map(DeviceFixtureStore::deviceJson).orElse(null);
    }

    private static String deviceJson(LightDevice e) {
        return "{"
            + "\"id\":\"" + ConfigBodyParser.jsonEscape(e.id()) + "\","
            + "\"name\":\"" + ConfigBodyParser.jsonEscape(e.name()) + "\","
            + "\"type\":\"" + ConfigBodyParser.jsonEscape(e.type()) + "\","
            + "\"room\":\"" + ConfigBodyParser.jsonEscape(e.room()) + "\","
            + "\"power\":" + e.power() + ","
            + "\"brightness\":" + formatBrightness(e.brightness()) + ","
            + "\"reachable\":" + e.reachable()
            + "}";
    }

    private static String formatBrightness(double b) {
        if (b == (long) b) {
            return String.valueOf((long) b);
        }
        return String.valueOf(b);
    }
}
