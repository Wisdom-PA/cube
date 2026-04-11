package wisdom.cube.voice;

import wisdom.cube.core.AutomationEngine;
import wisdom.cube.core.LlmService;
import wisdom.cube.core.SttService;
import wisdom.cube.core.TtsService;
import wisdom.cube.dialogue.DialogueManager;
import wisdom.cube.intent.IntentClassification;
import wisdom.cube.intent.IntentClassifier;
import wisdom.cube.memory.MemoryStore;

import java.util.Optional;

/**
 * Wake → STT → intent → (optional clarification) → LLM → TTS without calling {@link wisdom.cube.core.AutomationEngine}
 * (Phase 6). Uses pluggable services so hardware engines swap in later.
 */
public final class VoiceTurnPipeline {

    private final SttService stt;
    private final TtsService tts;
    private final LlmService llm;
    private final IntentClassifier classifier;
    private final DialogueManager dialogue;
    private final MemoryStore memoryStore;
    private final String defaultProfileId;

    public VoiceTurnPipeline(
        SttService stt,
        TtsService tts,
        LlmService llm,
        IntentClassifier classifier,
        DialogueManager dialogue,
        MemoryStore memoryStore,
        String defaultProfileId
    ) {
        this.stt = stt;
        this.tts = tts;
        this.llm = llm;
        this.classifier = classifier;
        this.dialogue = dialogue;
        this.memoryStore = memoryStore;
        this.defaultProfileId = defaultProfileId;
    }

    /**
     * After wake: pull one transcript from STT, run NLU + LLM + TTS.
     */
    public VoiceTurnResult runTurnAfterWake() {
        dialogue.onWake();
        Optional<String> text = stt.transcribe();
        if (text.isEmpty() || text.get().isBlank()) {
            dialogue.onListenTimeout();
            tts.speak("I did not catch that. Could you say it again?");
            return VoiceTurnResult.error("empty_transcript");
        }
        return processUtterance(text.get(), Optional.empty());
    }

    /**
     * Process text directly (tests / app relay). If the classifier asks for clarification and
     * {@code clarificationReply} is present, classifies again once (F4.T3 one follow-up).
     */
    public VoiceTurnResult processUtterance(String transcript, Optional<String> clarificationReply) {
        dialogue.onTranscriptReceived();
        memoryStore.remember(defaultProfileId, "last_utterance", transcript.trim());

        IntentClassification first = classifier.classify(transcript.trim());
        IntentClassification resolved = first;
        if (first instanceof IntentClassification.NeedsClarification n) {
            dialogue.onClarificationPrompt();
            tts.speak(n.questionForUser());
            if (clarificationReply.isEmpty() || clarificationReply.get().isBlank()) {
                return VoiceTurnResult.error("needs_clarification");
            }
            resolved = classifier.classify(clarificationReply.get().trim());
        }

        if (resolved instanceof IntentClassification.Resolved r) {
            return respondWithLlm(r.intent());
        }
        if (resolved instanceof IntentClassification.NeedsClarification) {
            tts.speak("Try again in the app.");
            dialogue.onUnknownIntent();
            return VoiceTurnResult.error("still_ambiguous");
        }
        tts.speak("I am not sure what you meant.");
        dialogue.onUnknownIntent();
        return VoiceTurnResult.error("unknown_intent");
    }

    private VoiceTurnResult respondWithLlm(AutomationEngine.Intent intent) {
        dialogue.onResolvedIntent();
        String prompt = LlmPromptBuilder.forIntent(intent);
        Optional<String> answer = llm.complete(prompt);
        if (answer.isEmpty()) {
            tts.speak("Sorry, I could not answer that.");
            dialogue.onSpokenResponse();
            return VoiceTurnResult.error("llm_empty");
        }
        String spoken = answer.get().trim();
        tts.speak(spoken);
        dialogue.onSpokenResponse();
        return VoiceTurnResult.ok(spoken);
    }
}
