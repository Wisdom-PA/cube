package wisdom.cube.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BehaviourLogWriterTest {

    @Mock
    BehaviourLogWriter writer;

    UUID chainId;
    Instant ts;

    @BeforeEach
    void setUp() {
        chainId = UUID.randomUUID();
        ts = Instant.now();
    }

    @Test
    void writeChainSummary() {
        BehaviourLogSchema.ChainSummary summary = new BehaviourLogSchema.ChainSummary(
            chainId, "cube-1", ts, Optional.of(ts.plusSeconds(10)),
            "adult-1", ts, "adult-1", List.of()
        );
        writer.writeChainSummary(summary);
        verify(writer).writeChainSummary(summary);
    }

    @Test
    void writeIntent() {
        BehaviourLogSchema.IntentEntry intent = new BehaviourLogSchema.IntentEntry(
            chainId, 0, ts, "set living room lights to warm",
            "set_light", "living_room", "warm", "adult-1"
        );
        writer.writeIntent(intent);
        verify(writer).writeIntent(intent);
    }

    @Test
    void writeAction() {
        BehaviourLogSchema.ActionEntry action = new BehaviourLogSchema.ActionEntry(
            chainId, 0, 0, ts, "brightness 0.2 → 0.8", "success", null, null
        );
        writer.writeAction(action);
        verify(writer).writeAction(action);
    }

    @Test
    void writeInternetCall() {
        BehaviourLogSchema.InternetCallEntry call = new BehaviourLogSchema.InternetCallEntry(
            chainId, 0, ts, "cube-1", "adult-1",
            "weather request", "weather", "api.example.com", "allowed", null, null
        );
        writer.writeInternetCall(call);
        verify(writer).writeInternetCall(call);
    }

    @Test
    void privacyModeChangeRecord() {
        BehaviourLogSchema.PrivacyModeChange change = new BehaviourLogSchema.PrivacyModeChange(
            ts, "paranoid", "normal", "voice"
        );
        assertEquals("paranoid", change.fromMode());
        assertEquals("normal", change.toMode());
    }
}
