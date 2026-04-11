package wisdom.cube.dialogue;

/**
 * High-level dialogue states (F4.T3.S1). Phase 6 stops before device execution.
 */
public enum DialogueState {
    IDLE,
    LISTENING,
    INTERPRETING,
    CLARIFYING,
    /** Ready to speak model output (no automation in Phase 6). */
    RESPONDING,
    ERROR
}
