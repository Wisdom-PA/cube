package wisdom.cube.dialogue;

/**
 * High-level dialogue states (F4.T3.S1). {@link #EXECUTING} covers automation and LLM work until TTS finishes.
 */
public enum DialogueState {
    IDLE,
    LISTENING,
    INTERPRETING,
    CLARIFYING,
    /** Running device command or on-device / cloud inference before spoken response. */
    EXECUTING,
    ERROR
}
