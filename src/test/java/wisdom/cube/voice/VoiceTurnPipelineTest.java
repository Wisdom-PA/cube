package wisdom.cube.voice;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import wisdom.cube.core.AutomationEngine;
import wisdom.cube.internet.CloudFallbackLlmService;
import wisdom.cube.internet.DefaultInternetAccessGate;
import wisdom.cube.internet.DefaultProfileInternetPolicy;
import wisdom.cube.internet.InternetAccessGate;
import wisdom.cube.internet.SessionInternetConsent;
import wisdom.cube.internet.StubCloudLlmClient;
import wisdom.cube.internet.VoiceCloudConsent;
import wisdom.cube.core.LlmService;
import wisdom.cube.core.SttService;
import wisdom.cube.core.TtsService;
import wisdom.cube.device.DefaultAutomationEngine;
import wisdom.cube.device.InMemoryLightDeviceRegistry;
import wisdom.cube.dialogue.DialogueManager;
import wisdom.cube.dialogue.SensitiveActionConfirmationPolicy;
import wisdom.cube.intent.IntentClassification;
import wisdom.cube.intent.IntentClassifier;
import wisdom.cube.intent.RuleBasedIntentClassifier;
import wisdom.cube.logging.InMemoryBehaviourLogStore;
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

    private VoiceTurnPipeline pipelineWithAutomation(InMemoryLightDeviceRegistry registry) {
        return new VoiceTurnPipeline(
            stt,
            tts,
            llm,
            new RuleBasedIntentClassifier(),
            new DialogueManager(),
            new InMemoryMemoryStore(),
            "adult-1",
            Optional.of(new DefaultAutomationEngine(registry)),
            Optional.empty()
        );
    }

    private VoiceTurnPipeline pipelineWithAutomation(InMemoryLightDeviceRegistry registry, InMemoryBehaviourLogStore log) {
        return new VoiceTurnPipeline(
            stt,
            tts,
            llm,
            new RuleBasedIntentClassifier(),
            new DialogueManager(),
            new InMemoryMemoryStore(),
            "adult-1",
            Optional.of(new DefaultAutomationEngine(registry)),
            Optional.of(log)
        );
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
    void resolvedIntentCallsLlmAndTtsWhenNoAutomation() {
        DefaultPipelineFixture f = defaultFixture();
        when(stt.transcribe()).thenReturn(Optional.of("turn on living room light"));
        when(llm.complete(anyString())).thenReturn(Optional.of("Living room light is on."));
        VoiceTurnResult r = f.pipeline().runTurnAfterWake();
        assertTrue(r.ok());
        assertEquals("Living room light is on.", r.spokenText().orElseThrow());
        verify(tts).speak("Living room light is on.");
        verify(llm).complete(anyString());
        assertTrue(f.memory().recall("adult-1", "last_utterance").isPresent());
    }

    @Test
    void lightIntentRunsAutomationWithoutLlm() {
        InMemoryLightDeviceRegistry registry = new InMemoryLightDeviceRegistry();
        VoiceTurnPipeline p = pipelineWithAutomation(registry);
        when(stt.transcribe()).thenReturn(Optional.of("turn on living room light"));
        VoiceTurnResult r = p.runTurnAfterWake();
        assertTrue(r.ok());
        assertTrue(registry.get("light-1").orElseThrow().power());
        verify(llm, never()).complete(anyString());
        verify(tts).speak("Okay.");
    }

    @Test
    void lightIntentWritesBehaviourLogWhenStoreProvided() {
        InMemoryLightDeviceRegistry registry = new InMemoryLightDeviceRegistry();
        InMemoryBehaviourLogStore log = new InMemoryBehaviourLogStore();
        VoiceTurnPipeline p = pipelineWithAutomation(registry, log);
        VoiceTurnResult r = p.processUtterance("turn off kitchen light", Optional.empty());
        assertTrue(r.ok());
        assertFalse(registry.get("light-2").orElseThrow().power());
        String json = log.toLogQueryJson();
        assertTrue(json.contains("set_light"));
        assertTrue(json.contains("kitchen"));
    }

    @Test
    void automationFailureSpeaksUserMessage() {
        IntentClassifier cl = mock(IntentClassifier.class);
        when(cl.classify(anyString())).thenReturn(
            new IntentClassification.Resolved(new AutomationEngine.Intent("set_light", "attic", "on")));
        VoiceTurnPipeline p = new VoiceTurnPipeline(
            stt,
            tts,
            llm,
            cl,
            new DialogueManager(),
            new InMemoryMemoryStore(),
            "adult-1",
            Optional.of(new DefaultAutomationEngine(new InMemoryLightDeviceRegistry())),
            Optional.empty()
        );
        VoiceTurnResult r = p.processUtterance("turn on attic light", Optional.empty());
        assertFalse(r.ok());
        assertEquals("automation_failed", r.code());
        verify(tts).speak("I could not find a light in that room.");
        verify(llm, never()).complete(anyString());
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

    @Test
    void automationUnreachableUsesStandardVoiceLine() {
        AutomationEngine auto = mock(AutomationEngine.class);
        when(auto.execute(any())).thenReturn(
            Optional.of(AutomationEngine.ActionResult.failure("UNREACHABLE", "Device unreachable")));
        IntentClassifier cl = mock(IntentClassifier.class);
        when(cl.classify(anyString())).thenReturn(
            new IntentClassification.Resolved(new AutomationEngine.Intent("set_light", "living_room", "on")));
        VoiceTurnPipeline p = new VoiceTurnPipeline(
            stt,
            tts,
            llm,
            cl,
            new DialogueManager(),
            new InMemoryMemoryStore(),
            "adult-1",
            Optional.of(auto),
            Optional.empty()
        );
        when(stt.transcribe()).thenReturn(Optional.of("turn on living room light"));
        VoiceTurnResult r = p.runTurnAfterWake();
        assertFalse(r.ok());
        verify(tts).speak("I could not reach that device right now.");
        verify(llm, never()).complete(anyString());
    }

    @Test
    void voiceContextChainResolvesTurnItOffAfterSuccessfulAutomation() {
        AtomicLong clock = new AtomicLong(0L);
        VoiceContextChain ctx = new VoiceContextChain(clock::get);
        InMemoryLightDeviceRegistry registry = new InMemoryLightDeviceRegistry();
        VoiceTurnPipeline p = new VoiceTurnPipeline(
            stt,
            tts,
            llm,
            new RuleBasedIntentClassifier(),
            new DialogueManager(clock::get),
            new InMemoryMemoryStore(),
            "adult-1",
            Optional.of(new DefaultAutomationEngine(registry)),
            Optional.empty(),
            Optional.of(ctx),
            Optional.empty()
        );
        assertTrue(p.processUtterance("turn on living room light", Optional.empty()).ok());
        assertTrue(registry.get("light-1").orElseThrow().power());
        clock.addAndGet(10_000L);
        assertTrue(p.processUtterance("turn it off", Optional.empty()).ok());
        assertFalse(registry.get("light-1").orElseThrow().power());
        verify(llm, never()).complete(anyString());
    }

    @Test
    void sensitiveConfirmationPolicyBlocksAutomation() {
        SensitiveActionConfirmationPolicy confirmAll = intent -> true;
        IntentClassifier cl = mock(IntentClassifier.class);
        when(cl.classify(anyString())).thenReturn(
            new IntentClassification.Resolved(new AutomationEngine.Intent("set_light", "living_room", "on")));
        VoiceTurnPipeline p = new VoiceTurnPipeline(
            stt,
            tts,
            llm,
            cl,
            new DialogueManager(),
            new InMemoryMemoryStore(),
            "adult-1",
            Optional.of(new DefaultAutomationEngine(new InMemoryLightDeviceRegistry())),
            Optional.empty(),
            Optional.empty(),
            Optional.of(confirmAll)
        );
        VoiceTurnResult r = p.processUtterance("turn on living room light", Optional.empty());
        assertFalse(r.ok());
        assertEquals("confirmation_required", r.code());
        verify(tts).speak("For safety, please confirm that action in the mobile app.");
    }

    @Test
    void automationExecuteReturnsEmptyOptional() {
        AutomationEngine auto = mock(AutomationEngine.class);
        when(auto.execute(any())).thenReturn(Optional.empty());
        IntentClassifier cl = mock(IntentClassifier.class);
        when(cl.classify(anyString())).thenReturn(
            new IntentClassification.Resolved(new AutomationEngine.Intent("set_light", "living_room", "on")));
        VoiceTurnPipeline p = new VoiceTurnPipeline(
            stt,
            tts,
            llm,
            cl,
            new DialogueManager(),
            new InMemoryMemoryStore(),
            "adult-1",
            Optional.of(auto),
            Optional.empty()
        );
        VoiceTurnResult r = p.processUtterance("turn on living room light", Optional.empty());
        assertFalse(r.ok());
        assertEquals("automation_empty", r.code());
        verify(tts).speak("I could not reach that device.");
    }

    @Test
    void paranoidModeUsesCloudWhenExplicitAllow() {
        AtomicReference<VoiceCloudConsent> ref = new AtomicReference<>(VoiceCloudConsent.UNSET);
        InternetAccessGate gate = new DefaultInternetAccessGate(
            () -> false,
            () -> "paranoid",
            new SessionInternetConsent(),
            new DefaultProfileInternetPolicy(),
            ref
        );
        LlmService cloudAwareLlm = new CloudFallbackLlmService(
            p -> Optional.of("on-device"),
            Optional.of(new StubCloudLlmClient("cloud-answer")),
            gate,
            "adult-1",
            Optional.empty(),
            UUID::randomUUID
        );
        IntentClassifier cl = mock(IntentClassifier.class);
        when(cl.classify(anyString())).thenReturn(
            new IntentClassification.Resolved(new AutomationEngine.Intent("question", "none", "weather")));
        VoiceTurnPipeline p = new VoiceTurnPipeline(
            stt,
            tts,
            cloudAwareLlm,
            cl,
            new DialogueManager(),
            new InMemoryMemoryStore(),
            "adult-1",
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(ref)
        );
        VoiceTurnResult r = p.processUtterance("what is the weather", Optional.empty(), Optional.of(true));
        assertTrue(r.ok());
        assertEquals("cloud-answer", r.spokenText().orElseThrow());
    }

    @Test
    void paranoidModeWithoutAllowUsesOnDevice() {
        AtomicReference<VoiceCloudConsent> ref = new AtomicReference<>(VoiceCloudConsent.UNSET);
        InternetAccessGate gate = new DefaultInternetAccessGate(
            () -> false,
            () -> "paranoid",
            new SessionInternetConsent(),
            new DefaultProfileInternetPolicy(),
            ref
        );
        LlmService cloudAwareLlm = new CloudFallbackLlmService(
            p -> Optional.of("on-device"),
            Optional.of(new StubCloudLlmClient("cloud-answer")),
            gate,
            "adult-1",
            Optional.empty(),
            UUID::randomUUID
        );
        IntentClassifier cl = mock(IntentClassifier.class);
        when(cl.classify(anyString())).thenReturn(
            new IntentClassification.Resolved(new AutomationEngine.Intent("question", "none", "weather")));
        VoiceTurnPipeline p = new VoiceTurnPipeline(
            stt,
            tts,
            cloudAwareLlm,
            cl,
            new DialogueManager(),
            new InMemoryMemoryStore(),
            "adult-1",
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(ref)
        );
        VoiceTurnResult r = p.processUtterance("what is the weather", Optional.empty());
        assertTrue(r.ok());
        assertEquals("on-device", r.spokenText().orElseThrow());
    }

    @Test
    void listenDeadlineAfterSlowTranscribeReturnsError() {
        AtomicLong clock = new AtomicLong(0L);
        DialogueManager dialogue = new DialogueManager(clock::get);
        when(stt.transcribe()).thenAnswer(inv -> {
            clock.addAndGet(DialogueManager.LISTEN_TIMEOUT_MS + 1);
            return Optional.of("turn on living room light");
        });
        VoiceTurnPipeline p = new VoiceTurnPipeline(
            stt,
            tts,
            llm,
            new RuleBasedIntentClassifier(),
            dialogue,
            new InMemoryMemoryStore(),
            "adult-1"
        );
        VoiceTurnResult r = p.runTurnAfterWake();
        assertFalse(r.ok());
        assertEquals("listen_deadline", r.code());
    }
}
