package wisdom.cube.intent;

import wisdom.cube.core.AutomationEngine;
import wisdom.cube.device.LightDevice;
import wisdom.cube.device.LightDeviceRegistry;

import java.util.List;
import java.util.Locale;

/**
 * When exactly one light is registered, map generic “the light” phrasing (F4.T2.S3).
 */
public final class SingleDeviceFallbackIntentClassifier implements IntentClassifier {

    private final LightDeviceRegistry registry;
    private final IntentClassifier downstream;

    public SingleDeviceFallbackIntentClassifier(LightDeviceRegistry registry, IntentClassifier downstream) {
        this.registry = registry;
        this.downstream = downstream;
    }

    @Override
    public IntentClassification classify(String transcript) {
        IntentClassification inner = downstream.classify(transcript);
        if (!(inner instanceof IntentClassification.Unknown)) {
            return inner;
        }
        List<LightDevice> lights = registry.allInOrder();
        if (lights.size() != 1) {
            return inner;
        }
        String t = transcript.toLowerCase(Locale.ROOT);
        if (!t.contains("light")) {
            return inner;
        }
        LightDevice only = lights.get(0);
        String roomSlug = roomDisplayToSlug(only.room());
        if (t.contains("off")) {
            return new IntentClassification.Resolved(
                new AutomationEngine.Intent("set_light", roomSlug, "off")
            );
        }
        if (t.contains("on")) {
            return new IntentClassification.Resolved(
                new AutomationEngine.Intent("set_light", roomSlug, "on")
            );
        }
        return inner;
    }

    static String roomDisplayToSlug(String displayRoom) {
        if (displayRoom == null || displayRoom.isBlank()) {
            return "unknown";
        }
        return displayRoom.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", "_");
    }
}
