package wisdom.cube.voice;

import wisdom.cube.core.AutomationEngine;
import wisdom.cube.core.LlmService;
import wisdom.cube.core.SttService;
import wisdom.cube.core.TtsService;
import wisdom.cube.dialogue.DialogueManager;
import wisdom.cube.dialogue.SensitiveActionConfirmationPolicy;
import wisdom.cube.internet.VoiceCloudConsent;
import wisdom.cube.intent.IntentClassification;
import wisdom.cube.intent.IntentClassifier;
import wisdom.cube.logging.BehaviourLogWriter;
import wisdom.cube.memory.MemoryStore;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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
    private final Optional<VoiceContextChain> voiceContext;
    private final Optional<SensitiveActionConfirmationPolicy> confirmationPolicy;
    private final Optional<AtomicReference<VoiceCloudConsent>> voiceCloudConsentSlot;

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
            Optional.empty(),
            Optional.empty(),
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
        this(
            stt,
            tts,
            llm,
            classifier,
            dialogue,
            memoryStore,
            defaultProfileId,
            automation,
            behaviourLog,
            Optional.empty(),
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
        Optional<BehaviourLogWriter> behaviourLog,
        Optional<VoiceContextChain> voiceContext,
        Optional<SensitiveActionConfirmationPolicy> confirmationPolicy
    ) {
        this(
            stt,
            tts,
            llm,
            classifier,
            dialogue,
            memoryStore,
            defaultProfileId,
            automation,
            behaviourLog,
            voiceContext,
            confirmationPolicy,
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
        Optional<BehaviourLogWriter> behaviourLog,
        Optional<VoiceContextChain> voiceContext,
        Optional<SensitiveActionConfirmationPolicy> confirmationPolicy,
        Optional<AtomicReference<VoiceCloudConsent>> voiceCloudConsentSlot
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
        this.voiceContext = voiceContext == null ? Optional.empty() : voiceContext;
        this.confirmationPolicy = confirmationPolicy == null ? Optional.empty() : confirmationPolicy;
        this.voiceCloudConsentSlot = voiceCloudConsentSlot == null ? Optional.empty() : voiceCloudConsentSlot;
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
        if (dialogue.listenDeadlineExceeded()) {
            dialogue.onListenTimeout();
            tts.speak("I did not catch that. Could you say it again?");
            return VoiceTurnResult.error("listen_deadline");
        }
        return processUtterance(text.get(), Optional.empty());
    }

    /**
     * Process text directly (tests / app relay). If the classifier asks for clarification and
     * {@code clarificationReply} is present, classifies again once (F4.T3 one follow-up).
     */
    public VoiceTurnResult processUtterance(String transcript, Optional<String> clarificationReply) {
        return processUtterance(transcript, clarificationReply, Optional.empty());
    }

    /**
     * Same as {@link #processUtterance(String, Optional)} with explicit cloud consent for this turn (F5.T2.S1).
     * When {@code allowCloud} is empty, voice cloud consent stays {@link VoiceCloudConsent#UNSET}.
     */
    public VoiceTurnResult processUtterance(
        String transcript,
        Optional<String> clarificationReply,
        Optional<Boolean> allowCloudForThisTurn
    ) {
        Optional<AtomicReference<VoiceCloudConsent>> slot = voiceCloudConsentSlot;
        if (slot.isPresent()) {
            AtomicReference<VoiceCloudConsent> ref = slot.get();
            VoiceCloudConsent previous = ref.get();
            if (allowCloudForThisTurn.isEmpty()) {
                ref.set(VoiceCloudConsent.UNSET);
            } else {
                ref.set(Boolean.TRUE.equals(allowCloudForThisTurn.get())
                    ? VoiceCloudConsent.ALLOW
                    : VoiceCloudConsent.DENY);
            }
            try {
                return doProcessUtterance(transcript, clarificationReply);
            } finally {
                ref.set(previous);
            }
        }
        return doProcessUtterance(transcript, clarificationReply);
    }

    private VoiceTurnResult doProcessUtterance(String transcript, Optional<String> clarificationReply) {
        dialogue.onTranscriptReceived();
        memoryStore.remember(defaultProfileId, "last_utterance", transcript.trim());

        IntentClassification first = resolveClassification(transcript.trim(), clarificationReply.isPresent());
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

    private IntentClassification resolveClassification(String trimmedTranscript, boolean isClarificationReply) {
        if (!isClarificationReply) {
            Optional<AutomationEngine.Intent> fromContext = voiceContext.flatMap(
                v -> v.resolveFollowUp(trimmedTranscript)
            );
            if (fromContext.isPresent()) {
                return new IntentClassification.Resolved(fromContext.get());
            }
        }
        return classifier.classify(trimmedTranscript);
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
        if (confirmationPolicy.isPresent() && confirmationPolicy.get().requiresVoiceConfirmation(intent)) {
            String msg = "For safety, please confirm that action in the mobile app.";
            tts.speak(msg);
            dialogue.onSpokenResponse();
            return VoiceTurnResult.error("confirmation_required", msg);
        }
        Optional<AutomationEngine.ActionResult> ar = automation.get().execute(intent);
        if (ar.isEmpty()) {
            String msg = "I could not reach that device.";
            tts.speak(msg);
            dialogue.onSpokenResponse();
            return VoiceTurnResult.error("automation_empty", msg);
        }
        AutomationEngine.ActionResult result = ar.get();
        if (result.success()) {
            voiceContext.ifPresent(v -> v.recordAutomationSuccess(intent));
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
            case "UNREACHABLE" -> "I could not reach that device right now.";
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
