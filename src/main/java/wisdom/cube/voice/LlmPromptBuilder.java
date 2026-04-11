package wisdom.cube.voice;

import wisdom.cube.core.AutomationEngine;

/**
 * Builds on-device LLM prompts from parsed intents (F4.T1.S3 scaffold).
 */
public final class LlmPromptBuilder {

    private LlmPromptBuilder() {
    }

    public static String forIntent(AutomationEngine.Intent intent) {
        return "You are Wisdom, a concise home assistant. The user intent is type="
            + intent.type()
            + ", targets="
            + intent.targets()
            + ", parameters="
            + intent.parameters()
            + ". Reply in one short spoken sentence. Do not claim actions were executed.";
    }
}
