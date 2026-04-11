package wisdom.cube.dialogue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals(DialogueState.RESPONDING, d.state());
        d.onSpokenResponse();
        assertEquals(DialogueState.IDLE, d.state());
        d.onListenTimeout();
        assertEquals(DialogueState.ERROR, d.state());
        d.reset();
        d.onUnknownIntent();
        assertEquals(DialogueState.ERROR, d.state());
    }
}
