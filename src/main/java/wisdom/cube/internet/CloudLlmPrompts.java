package wisdom.cube.internet;

/**
 * Prompt shaping and user-facing copy for cloud paths (F5.T1.S2).
 */
public final class CloudLlmPrompts {

    public static final String CLOUD_UNAVAILABLE_SPOKEN_LINE =
        "I couldn't reach the online service right now, there are more details in the app";

    private CloudLlmPrompts() {
    }

    /**
     * Wrap on-device prompt for cloud: short, no extra home metadata beyond what is already in {@code prompt}.
     */
    public static String cloudUserMessage(String prompt) {
        String p = prompt == null ? "" : prompt.trim();
        if (p.length() > 2_000) {
            p = p.substring(0, 2_000);
        }
        return "Answer in one or two short neutral sentences suitable for text-to-speech. "
            + "Do not invent device actions. If unsure, say you are unsure.\n\n"
            + p;
    }
}
