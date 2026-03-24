package wisdom.cube.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoreInterfacesTest {

    @Mock
    SttService stt;

    @Mock
    TtsService tts;

    @Mock
    LlmService llm;

    @Mock
    AutomationEngine automation;

    @Mock
    ApiGateway apiGateway;

    @Test
    void sttServiceCanBeMocked() {
        when(stt.transcribe()).thenReturn(Optional.of("turn on the lights"));
        assertEquals(Optional.of("turn on the lights"), stt.transcribe());
    }

    @Test
    void ttsServiceCanBeMocked() {
        tts.speak("Hello");
        verify(tts).speak("Hello");
    }

    @Test
    void llmServiceCanBeMocked() {
        when(llm.complete(anyString())).thenReturn(Optional.of("OK"));
        assertEquals(Optional.of("OK"), llm.complete("prompt"));
    }

    @Test
    void automationEngineIntentAndActionResult() {
        AutomationEngine.Intent intent = new AutomationEngine.Intent("set_brightness", "living_room", "0.8");
        when(automation.execute(intent)).thenReturn(Optional.of(AutomationEngine.ActionResult.ok()));

        Optional<AutomationEngine.ActionResult> result = automation.execute(intent);
        assertTrue(result.isPresent());
        assertTrue(result.get().success());
        assertNull(result.get().errorCode());

        AutomationEngine.ActionResult failure = AutomationEngine.ActionResult.failure("TIMEOUT", "Device unreachable");
        assertFalse(failure.success());
        assertEquals("TIMEOUT", failure.errorCode());
    }

    @Test
    void apiGatewayCanBeMocked() {
        apiGateway.start();
        apiGateway.stop();
        verify(apiGateway).start();
        verify(apiGateway).stop();
    }
}
