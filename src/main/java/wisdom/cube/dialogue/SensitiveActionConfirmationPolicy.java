package wisdom.cube.dialogue;

import wisdom.cube.core.AutomationEngine;

/**
 * Future sensitive domains (locks, doors, …) require explicit confirmation (F4.T3.S3); lights do not.
 */
public interface SensitiveActionConfirmationPolicy {

    boolean requiresVoiceConfirmation(AutomationEngine.Intent intent);
}
