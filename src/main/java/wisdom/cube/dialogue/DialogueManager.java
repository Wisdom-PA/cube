package wisdom.cube.dialogue;

import java.util.function.LongSupplier;

/**
 * Tracks dialogue state for the voice pipeline (F4.T3). Not thread-safe; one session per orchestrator.
 * {@link #onWake()} starts a {@value #LISTEN_TIMEOUT_MS} listen window ({@link #listenDeadlineExceeded}).
 */
public final class DialogueManager {

    public static final long LISTEN_TIMEOUT_MS = 5_000L;

    private final LongSupplier epochMillis;
    private DialogueState state = DialogueState.IDLE;
    private long listenDeadlineMillis = Long.MAX_VALUE;

    public DialogueManager() {
        this(System::currentTimeMillis);
    }

    public DialogueManager(LongSupplier epochMillis) {
        this.epochMillis = epochMillis;
    }

    public DialogueState state() {
        return state;
    }

    public void reset() {
        state = DialogueState.IDLE;
        listenDeadlineMillis = Long.MAX_VALUE;
    }

    public void onWake() {
        state = DialogueState.LISTENING;
        listenDeadlineMillis = epochMillis.getAsLong() + LISTEN_TIMEOUT_MS;
    }

    /**
     * While {@link DialogueState#LISTENING}, true after {@value #LISTEN_TIMEOUT_MS} since {@link #onWake()}.
     */
    public boolean listenDeadlineExceeded() {
        return state == DialogueState.LISTENING && epochMillis.getAsLong() > listenDeadlineMillis;
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
        state = DialogueState.EXECUTING;
    }

    public void onSpokenResponse() {
        state = DialogueState.IDLE;
    }

    public void onUnknownIntent() {
        state = DialogueState.ERROR;
    }
}
