package wisdom.cube.internet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudLlmPromptsTest {

    @Test
    void truncatesLongPrompt() {
        String longPrompt = "x".repeat(3_000);
        String out = CloudLlmPrompts.cloudUserMessage(longPrompt);
        assertTrue(out.length() < longPrompt.length() + 500);
    }

    @Test
    void standardFailureLineIsStable() {
        assertTrue(CloudLlmPrompts.CLOUD_UNAVAILABLE_SPOKEN_LINE.contains("online service"));
    }
}
