package wisdom.cube.device;

import wisdom.cube.core.AutomationEngine;

import java.util.Locale;
import java.util.Optional;

/**
 * Executes light intents against a {@link LightDeviceRegistry} (mock integration for Phase 7.1).
 */
public final class DefaultAutomationEngine implements AutomationEngine {

    private final LightDeviceRegistry lights;

    public DefaultAutomationEngine(LightDeviceRegistry lights) {
        this.lights = lights;
    }

    @Override
    public Optional<ActionResult> execute(Intent intent) {
        if (intent == null) {
            return Optional.of(ActionResult.failure("BAD_REQUEST", "Intent is null"));
        }
        String type = intent.type();
        String room = intent.targets();
        String parameters = intent.parameters();
        if ("set_light".equals(type)) {
            return setLight(room, parameters);
        }
        if ("set_brightness".equals(type)) {
            return setBrightness(room, parameters);
        }
        return Optional.of(ActionResult.failure("UNSUPPORTED", "Unknown intent: " + type));
    }

    private Optional<ActionResult> setLight(String roomSlug, String parameters) {
        Optional<String> id = lights.firstLightIdInRoom(roomSlug);
        if (id.isEmpty()) {
            return Optional.of(ActionResult.failure("NOT_FOUND", "No light in room"));
        }
        String p = parameters == null ? "" : parameters.trim().toLowerCase(Locale.ROOT);
        if (!"on".equals(p) && !"off".equals(p)) {
            return Optional.of(ActionResult.failure("BAD_PARAM", "Expected on or off"));
        }
        lights.setPower(id.get(), "on".equals(p));
        return Optional.of(ActionResult.ok());
    }

    private Optional<ActionResult> setBrightness(String roomSlug, String parameters) {
        Optional<String> id = lights.firstLightIdInRoom(roomSlug);
        if (id.isEmpty()) {
            return Optional.of(ActionResult.failure("NOT_FOUND", "No light in room"));
        }
        if (parameters == null || parameters.isBlank()) {
            return Optional.of(ActionResult.failure("BAD_PARAM", "Missing brightness"));
        }
        double level;
        try {
            level = Double.parseDouble(parameters.trim());
        } catch (NumberFormatException e) {
            return Optional.of(ActionResult.failure("BAD_PARAM", "Invalid brightness"));
        }
        lights.setBrightness(id.get(), level);
        return Optional.of(ActionResult.ok());
    }
}
