package wisdom.cube.dialogue;

import wisdom.cube.core.AutomationEngine;

/**
 * Default: no confirmation for lights/brightness; confirmation for lock/door/gate/window style intents.
 */
public final class DefaultSensitiveActionConfirmationPolicy implements SensitiveActionConfirmationPolicy {

    @Override
    public boolean requiresVoiceConfirmation(AutomationEngine.Intent intent) {
        String type = intent.type() == null ? "" : intent.type().toLowerCase();
        return type.contains("lock")
            || type.contains("door")
            || type.contains("gate")
            || type.contains("window");
    }
}
