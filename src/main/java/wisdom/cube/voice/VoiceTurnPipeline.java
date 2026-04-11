package wisdom.cube.voice;

import wisdom.cube.core.AutomationEngine;
import wisdom.cube.core.LlmService;
import wisdom.cube.core.SttService;
import wisdom.cube.core.TtsService;
import wisdom.cube.dialogue.DialogueManager;
import wisdom.cube.intent.IntentClassification;
import wisdom.cube.intent.IntentClassifier;
import wisdom.cube.logging.BehaviourLogWriter;
import wisdom.cube.memory.MemoryStore;

import java.util.Optional;

/**
 * Wake → STT → intent → (optional clarification) → device automation and/or LLM → TTS.
 * When {@link AutomationEngine} is present, light intents ({@code set_light}, {@code set_brightness})
 * run on-device first; other resolved intents still use the LLM path.
 */
public final class VoiceTurnPipeline {

    private final SttService stt;
    private final TtsService tts;
    private final LlmService llm;
    private final IntentClassifier classifier;
    private final DialogueManager dialogue;
    private final MemoryStore memoryStore;
    private final String defaultProfileId;
    private final Optional<AutomationEngine> automation;
    private final Optional<BehaviourLogWriter> behaviourLog;

    public VoiceTurnPipeline(
        SttService stt,
        TtsService tts,
        LlmService llm,
        IntentClassifier classifier,
        DialogueManager dialogue,
        MemoryStore memoryStore,
        String defaultProfileId
    ) {
        this(
            stt,
            tts,
            llm,
            classifier,
            dialogue,
            memoryStore,
            defaultProfileId,
            Optional.empty(),
            Optional.empty()
        );
    }

    public VoiceTurnPipeline(
        SttService stt,
        TtsService tts,
        LlmService llm,
        IntentClassifier classifier,
        DialogueManager dialogue,
        MemoryStore memoryStore,
        String defaultProfileId,
        Optional<AutomationEngine> automation,
        Optional<BehaviourLogWriter> behaviourLog
    ) {
        this.stt = stt;
        this.tts = tts;
        this.llm = llm;
        this.classifier = classifier;
        this.dialogue = dialogue;
        this.memoryStore = memoryStore;
        this.defaultProfileId = defaultProfileId;
        this.automation = automation == null ? Optional.empty() : automation;
        this.behaviourLog = behaviourLog == null ? Optional.empty() : behaviourLog;
    }

    /**
     * After wake: pull one transcript from STT, run NLU + automation/LLM + TTS.
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
            return respondForResolvedIntent(transcript.trim(), r.intent());
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

    private VoiceTurnResult respondForResolvedIntent(String utteranceForLog, AutomationEngine.Intent intent) {
        dialogue.onResolvedIntent();
        if (isLightAutomationIntent(intent) && automation.isPresent()) {
            return runDeviceAutomation(utteranceForLog, intent);
        }
        return respondWithLlm(intent);
    }

    private static boolean isLightAutomationIntent(AutomationEngine.Intent intent) {
        String t = intent.type();
        return "set_light".equals(t) || "set_brightness".equals(t);
    }

    private VoiceTurnResult runDeviceAutomation(String utteranceForLog, AutomationEngine.Intent intent) {
        Optional<AutomationEngine.ActionResult> ar = automation.get().execute(intent);
        if (ar.isEmpty()) {
            String msg = "I could not reach that device.";
            tts.speak(msg);
            dialogue.onSpokenResponse();
            return VoiceTurnResult.error("automation_empty", msg);
        }
        AutomationEngine.ActionResult result = ar.get();
        if (result.success()) {
            String spoken = DeviceVoiceResponses.afterAutomationSuccess(intent);
            behaviourLog.ifPresent(log -> log.recordVoiceDeviceAutomation(
                defaultProfileId,
                utteranceForLog,
                intent,
                result,
                spoken
            ));
            tts.speak(spoken);
            dialogue.onSpokenResponse();
            return VoiceTurnResult.ok(spoken);
        }
        String errMsg = userFacingDeviceError(result);
        behaviourLog.ifPresent(log -> log.recordVoiceDeviceAutomation(
            defaultProfileId,
            utteranceForLog,
            intent,
            result,
            errMsg
        ));
        tts.speak(errMsg);
        dialogue.onSpokenResponse();
        return VoiceTurnResult.error("automation_failed", errMsg);
    }

    private static String userFacingDeviceError(AutomationEngine.ActionResult result) {
        String code = result.errorCode() == null ? "" : result.errorCode();
        return switch (code) {
            case "NOT_FOUND" -> "I could not find a light in that room.";
            case "BAD_PARAM" -> "That brightness or setting was not valid.";
            case "UNSUPPORTED" -> "I cannot do that yet.";
            default -> result.errorMessage() != null && !result.errorMessage().isBlank()
                ? result.errorMessage()
                : "Something went wrong with the device.";
        };
    }

    private VoiceTurnResult respondWithLlm(AutomationEngine.Intent intent) {
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
