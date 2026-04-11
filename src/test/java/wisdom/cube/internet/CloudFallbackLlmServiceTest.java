package wisdom.cube.internet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import wisdom.cube.core.LlmService;
import wisdom.cube.logging.BehaviourLogWriter;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CloudFallbackLlmServiceTest {

    @Mock
    BehaviourLogWriter behaviourLogWriter;

    @Test
    void skipsCloudWhenOptionalEmpty() {
        LlmService on = p -> Optional.of("local");
        CloudFallbackLlmService svc = new CloudFallbackLlmService(
            on,
            Optional.empty(),
            pid -> true,
            "p1",
            Optional.empty(),
            UUID::randomUUID
        );
        assertEquals("local", svc.complete("x").orElseThrow());
    }

    @Test
    void usesCloudWhenGateAllows() {
        LlmService on = p -> Optional.of("local");
        CloudFallbackLlmService svc = new CloudFallbackLlmService(
            on,
            Optional.of(new StubCloudLlmClient("cloud-reply")),
            pid -> true,
            "p1",
            Optional.empty(),
            UUID::randomUUID
        );
        assertEquals("cloud-reply", svc.complete("x").orElseThrow());
    }

    @Test
    void fallsBackWhenCloudFails() {
        LlmService on = p -> Optional.of("local");
        CloudFallbackLlmService svc = new CloudFallbackLlmService(
            on,
            Optional.of(new FailingCloudLlmClient()),
            pid -> true,
            "p1",
            Optional.empty(),
            UUID::randomUUID
        );
        assertEquals("local", svc.complete("x").orElseThrow());
    }

    @Test
    void logsInternetCallOnSuccess() {
        InternetCallLogger logger = new InternetCallLogger(behaviourLogWriter, "cube");
        LlmService on = p -> Optional.of("local");
        CloudFallbackLlmService svc = new CloudFallbackLlmService(
            on,
            Optional.of(new StubCloudLlmClient("ok")),
            pid -> true,
            "p1",
            Optional.of(logger),
            () -> UUID.fromString("00000000-0000-0000-0000-000000000099")
        );
        svc.complete("prompt");
        verify(behaviourLogWriter).writeInternetCall(ArgumentMatchers.argThat(
            e -> "cloud_llm".equals(e.serviceCategory()) && "ok".equals(e.result())));
    }

    @Test
    void gateBlocksCloud() {
        AtomicReference<VoiceCloudConsent> v = new AtomicReference<>(VoiceCloudConsent.UNSET);
        InternetAccessGate gate = new DefaultInternetAccessGate(
            () -> false,
            () -> "paranoid",
            new SessionInternetConsent(),
            new DefaultProfileInternetPolicy(),
            v
        );
        LlmService on = p -> Optional.of("local");
        CloudFallbackLlmService svc = new CloudFallbackLlmService(
            on,
            Optional.of(new StubCloudLlmClient("cloud")),
            gate,
            "p1",
            Optional.empty(),
            UUID::randomUUID
        );
        assertEquals("local", svc.complete("x").orElseThrow());
    }
}
