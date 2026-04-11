package wisdom.cube.dialogue;

/**
 * Tracks dialogue state for the voice pipeline (F4.T3). Not thread-safe; one session per orchestrator.
 */
public final class DialogueManager {

    private DialogueState state = DialogueState.IDLE;

    public DialogueState state() {
        return state;
    }

    public void reset() {
        state = DialogueState.IDLE;
    }

    public void onWake() {
        state = DialogueState.LISTENING;
    }

    public void onTranscriptReceived() {
        state = DialogueState.INTERPRETING;
    }

    public void onClarificationPrompt() {
        state = DialogueState.CLARIFYING;
    }

    public void onListenTimeout() {
        state = DialogueState.ERROR;
    }

    public void onResolvedIntent() {
        state = DialogueState.RESPONDING;
    }

    public void onSpokenResponse() {
        state = DialogueState.IDLE;
    }

    public void onUnknownIntent() {
        state = DialogueState.ERROR;
    }
}
