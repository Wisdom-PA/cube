package wisdom.cube.voice;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import wisdom.cube.core.LlmService;
import wisdom.cube.core.SttService;
import wisdom.cube.core.TtsService;
import wisdom.cube.dialogue.DialogueManager;
import wisdom.cube.intent.IntentClassification;
import wisdom.cube.intent.IntentClassifier;
import wisdom.cube.intent.RuleBasedIntentClassifier;
import wisdom.cube.memory.InMemoryMemoryStore;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VoiceTurnPipelineTest {

    @Mock
    SttService stt;

    @Mock
    TtsService tts;

    @Mock
    LlmService llm;

    private record DefaultPipelineFixture(
        VoiceTurnPipeline pipeline,
        InMemoryMemoryStore memory,
        DialogueManager dialogue
    ) { }

    private DefaultPipelineFixture defaultFixture() {
        InMemoryMemoryStore memory = new InMemoryMemoryStore();
        DialogueManager dialogue = new DialogueManager();
        VoiceTurnPipeline pipeline = new VoiceTurnPipeline(
            stt,
            tts,
            llm,
            new RuleBasedIntentClassifier(),
            dialogue,
            memory,
            "adult-1"
        );
        return new DefaultPipelineFixture(pipeline, memory, dialogue);
    }

    @Test
    void runTurnAfterWakeEmptyTranscript() {
        DefaultPipelineFixture f = defaultFixture();
        when(stt.transcribe()).thenReturn(Optional.empty());
        VoiceTurnResult r = f.pipeline().runTurnAfterWake();
        assertFalse(r.ok());
        assertEquals("empty_transcript", r.code());
        verify(tts).speak(anyString());
    }

    @Test
    void resolvedIntentCallsLlmAndTts() {
        DefaultPipelineFixture f = defaultFixture();
        when(stt.transcribe()).thenReturn(Optional.of("turn on living room light"));
        when(llm.complete(anyString())).thenReturn(Optional.of("Living room light is on."));
        VoiceTurnResult r = f.pipeline().runTurnAfterWake();
        assertTrue(r.ok());
        assertEquals("Living room light is on.", r.spokenText().orElseThrow());
        verify(tts).speak("Living room light is on.");
        assertTrue(f.memory().recall("adult-1", "last_utterance").isPresent());
    }

    @Test
    void clarificationPath() {
        DefaultPipelineFixture f = defaultFixture();
        when(llm.complete(anyString())).thenReturn(Optional.of("Done."));
        VoiceTurnResult r = f.pipeline().processUtterance(
            "turn on the light",
            Optional.of("turn on living room light")
        );
        assertTrue(r.ok());
        verify(tts).speak("Which room?");
        verify(tts).speak("Done.");
    }

    @Test
    void clarificationMissingSecondLine() {
        DefaultPipelineFixture f = defaultFixture();
        VoiceTurnResult r = f.pipeline().processUtterance("turn on the light", Optional.empty());
        assertFalse(r.ok());
        assertEquals("needs_clarification", r.code());
    }

    @Test
    void llmEmpty() {
        DefaultPipelineFixture f = defaultFixture();
        when(stt.transcribe()).thenReturn(Optional.of("turn on living room light"));
        when(llm.complete(anyString())).thenReturn(Optional.empty());
        VoiceTurnResult r = f.pipeline().runTurnAfterWake();
        assertFalse(r.ok());
        assertEquals("llm_empty", r.code());
    }

    @Test
    void unknownIntent() {
        DefaultPipelineFixture f = defaultFixture();
        VoiceTurnResult r = f.pipeline().processUtterance("random gibberish xyz", Optional.empty());
        assertFalse(r.ok());
        assertEquals("unknown_intent", r.code());
    }

    @Test
    void stillAmbiguousAfterFollowUp() {
        IntentClassifier cl = mock(IntentClassifier.class);
        when(cl.classify(anyString()))
            .thenReturn(new IntentClassification.NeedsClarification("Which?"))
            .thenReturn(new IntentClassification.NeedsClarification("Still?"));
        VoiceTurnPipeline p = new VoiceTurnPipeline(
            stt,
            tts,
            llm,
            cl,
            new DialogueManager(),
            new InMemoryMemoryStore(),
            "adult-1"
        );
        VoiceTurnResult r = p.processUtterance("a", Optional.of("b"));
        assertFalse(r.ok());
        assertEquals("still_ambiguous", r.code());
    }
}
