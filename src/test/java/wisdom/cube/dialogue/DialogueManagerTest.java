package wisdom.cube.dialogue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueManagerTest {

    @Test
    void transitions() {
        DialogueManager d = new DialogueManager();
        assertEquals(DialogueState.IDLE, d.state());
        d.onWake();
        assertEquals(DialogueState.LISTENING, d.state());
        d.onTranscriptReceived();
        assertEquals(DialogueState.INTERPRETING, d.state());
        d.onClarificationPrompt();
        assertEquals(DialogueState.CLARIFYING, d.state());
        d.reset();
        assertEquals(DialogueState.IDLE, d.state());
        d.onResolvedIntent();
        assertEquals(DialogueState.EXECUTING, d.state());
        d.onSpokenResponse();
        assertEquals(DialogueState.IDLE, d.state());
        d.onListenTimeout();
        assertEquals(DialogueState.ERROR, d.state());
        d.reset();
        d.onUnknownIntent();
        assertEquals(DialogueState.ERROR, d.state());
    }

    @Test
    void listenDeadlineAfterFiveSeconds() {
        long[] now = {1_000L};
        DialogueManager d = new DialogueManager(() -> now[0]);
        d.onWake();
        assertFalse(d.listenDeadlineExceeded());
        now[0] = 1_000L + DialogueManager.LISTEN_TIMEOUT_MS;
        assertFalse(d.listenDeadlineExceeded());
        now[0] = 1_000L + DialogueManager.LISTEN_TIMEOUT_MS + 1;
        assertTrue(d.listenDeadlineExceeded());
    }

    @Test
    void listenDeadlineNotCheckedOutsideListening() {
        DialogueManager d = new DialogueManager(() -> 99_000L);
        assertFalse(d.listenDeadlineExceeded());
        d.onTranscriptReceived();
        d.onWake();
        d.onTranscriptReceived();
        assertFalse(d.listenDeadlineExceeded());
    }
}
