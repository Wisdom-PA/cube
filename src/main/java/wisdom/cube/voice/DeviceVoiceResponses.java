package wisdom.cube.voice;

import wisdom.cube.core.AutomationEngine;

/**
 * Short confirmations for successful light automation (Plan voice error copy; keep minimal).
 */
public final class DeviceVoiceResponses {

    private DeviceVoiceResponses() { }

    public static String afterAutomationSuccess(AutomationEngine.Intent intent) {
        if (intent == null) {
            return "Done.";
        }
        return switch (intent.type()) {
            case "set_light" -> "on".equalsIgnoreCase(intent.parameters()) ? "Okay." : "Done.";
            case "set_brightness" -> "Brightness updated.";
            default -> "Done.";
        };
    }
}
