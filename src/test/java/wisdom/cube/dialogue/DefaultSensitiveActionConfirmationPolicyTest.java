package wisdom.cube.dialogue;

import org.junit.jupiter.api.Test;
import wisdom.cube.core.AutomationEngine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultSensitiveActionConfirmationPolicyTest {

    private final DefaultSensitiveActionConfirmationPolicy policy = new DefaultSensitiveActionConfirmationPolicy();

    @Test
    void lightsDoNotRequireConfirmation() {
        assertFalse(policy.requiresVoiceConfirmation(
            new AutomationEngine.Intent("set_light", "living_room", "on")));
        assertFalse(policy.requiresVoiceConfirmation(
            new AutomationEngine.Intent("set_brightness", "kitchen", "0.5")));
        assertFalse(policy.requiresVoiceConfirmation(
            new AutomationEngine.Intent(null, "x", "y")));
    }

    @Test
    void lockAndDoorTypesRequireConfirmation() {
        assertTrue(policy.requiresVoiceConfirmation(
            new AutomationEngine.Intent("unlock_door", "front", "unlock")));
        assertTrue(policy.requiresVoiceConfirmation(
            new AutomationEngine.Intent("open_gate", "driveway", "open")));
        assertTrue(policy.requiresVoiceConfirmation(
            new AutomationEngine.Intent("open_window", "bedroom", "open")));
    }
}
