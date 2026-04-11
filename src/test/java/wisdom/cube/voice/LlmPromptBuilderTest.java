package wisdom.cube.voice;

import org.junit.jupiter.api.Test;
import wisdom.cube.core.AutomationEngine;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmPromptBuilderTest {

    @Test
    void includesIntentFields() {
        String p = LlmPromptBuilder.forIntent(new AutomationEngine.Intent("t", "tg", "pr"));
        assertTrue(p.contains("t"));
        assertTrue(p.contains("tg"));
        assertTrue(p.contains("pr"));
    }
}
