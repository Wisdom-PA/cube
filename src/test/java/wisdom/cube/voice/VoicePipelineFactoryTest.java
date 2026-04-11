package wisdom.cube.voice;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import wisdom.cube.core.LlmService;
import wisdom.cube.core.SttService;
import wisdom.cube.core.TtsService;
import wisdom.cube.device.InMemoryLightDeviceRegistry;
import wisdom.cube.dialogue.DialogueManager;
import wisdom.cube.gateway.DeviceFixtureStore;
import wisdom.cube.gateway.HttpServerGateway;
import wisdom.cube.intent.RuleBasedIntentClassifier;
import wisdom.cube.logging.InMemoryBehaviourLogStore;
import wisdom.cube.memory.InMemoryMemoryStore;

import java.util.Optional;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VoicePipelineFactoryTest {

    @Mock
    SttService stt;

    @Mock
    TtsService tts;

    @Mock
    LlmService llm;

    @Test
    void forGatewaySharesRegistryAndLogWithVoiceTurn() {
        InMemoryBehaviourLogStore log = new InMemoryBehaviourLogStore();
        DeviceFixtureStore devices = new DeviceFixtureStore(new InMemoryLightDeviceRegistry());
        HttpServerGateway gateway = new HttpServerGateway(0, Executors.newSingleThreadExecutor(), log, devices);

        when(stt.transcribe()).thenReturn(Optional.of("turn off kitchen light"));

        VoiceTurnPipeline pipeline = VoicePipelineFactory.forGateway(
            gateway,
            stt,
            tts,
            llm,
            new RuleBasedIntentClassifier(),
            new DialogueManager(),
            new InMemoryMemoryStore(),
            "adult-1"
        );
        assertTrue(pipeline.runTurnAfterWake().ok());
        assertFalse(devices.registry().get("light-2").orElseThrow().power());
        assertTrue(log.toLogQueryJson().contains("set_light"));
        verify(llm, never()).complete(anyString());
    }
}
