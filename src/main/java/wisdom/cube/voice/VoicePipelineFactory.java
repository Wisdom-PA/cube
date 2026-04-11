package wisdom.cube.voice;

import wisdom.cube.core.LlmService;
import wisdom.cube.core.SttService;
import wisdom.cube.core.TtsService;
import wisdom.cube.device.DefaultAutomationEngine;
import wisdom.cube.dialogue.DialogueManager;
import wisdom.cube.gateway.HttpServerGateway;
import wisdom.cube.intent.IntentClassifier;
import wisdom.cube.memory.MemoryStore;

import java.util.Optional;

/**
 * Builds a {@link VoiceTurnPipeline} that shares the running gateway's device registry and behaviour log
 * so voice control and the companion app see the same state.
 */
public final class VoicePipelineFactory {

    private VoicePipelineFactory() {
    }

    public static VoiceTurnPipeline forGateway(
        HttpServerGateway gateway,
        SttService stt,
        TtsService tts,
        LlmService llm,
        IntentClassifier intentClassifier,
        DialogueManager dialogue,
        MemoryStore memoryStore,
        String defaultProfileId
    ) {
        return new VoiceTurnPipeline(
            stt,
            tts,
            llm,
            intentClassifier,
            dialogue,
            memoryStore,
            defaultProfileId,
            Optional.of(new DefaultAutomationEngine(gateway.deviceStore().registry())),
            Optional.of(gateway.behaviourLog())
        );
    }
}
