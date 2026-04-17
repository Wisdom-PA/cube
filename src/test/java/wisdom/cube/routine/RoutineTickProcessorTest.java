package wisdom.cube.routine;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import wisdom.cube.device.InMemoryLightDeviceRegistry;
import wisdom.cube.logging.InMemoryBehaviourLogStore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutineTickProcessorTest {

    @Test
    void eveningRoutineRunsAt1800UtcAndLogs() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-17T18:00:00Z"), ZoneOffset.UTC);
        InMemoryLightDeviceRegistry reg = new InMemoryLightDeviceRegistry();
        reg.setPower("light-1", false);
        InMemoryBehaviourLogStore log = new InMemoryBehaviourLogStore();
        RoutineCatalog cat = new FixtureRoutineCatalog();
        RoutineTickProcessor proc = new RoutineTickProcessor(clock, cat, reg, log);
        proc.onTick();
        assertTrue(reg.get("light-1").orElseThrow().power());
        String json = log.toLogQueryJson();
        assertTrue(json.contains("routine.timer"));
        assertTrue(json.contains("Evening lights"));
    }

    @Test
    void morningRoutineRespectsTimeWindow() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-17T07:00:00Z"), ZoneOffset.UTC);
        InMemoryLightDeviceRegistry reg = new InMemoryLightDeviceRegistry();
        reg.setPower("light-2", false);
        InMemoryBehaviourLogStore log = new InMemoryBehaviourLogStore();
        RoutineCatalog cat = new FixtureRoutineCatalog();
        new RoutineTickProcessor(clock, cat, reg, log).onTick();
        assertTrue(reg.get("light-2").orElseThrow().power());
        assertTrue(log.toLogQueryJson().contains("Good morning"));
    }

    @Test
    void triggerMatchesButConditionWindowBlocksRun() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-17T07:00:00Z"), ZoneOffset.UTC);
        InMemoryLightDeviceRegistry reg = new InMemoryLightDeviceRegistry();
        reg.setPower("light-2", false);
        InMemoryBehaviourLogStore log = new InMemoryBehaviourLogStore();
        RoutineDefinition narrow = new RoutineDefinition(
            "rx",
            "morning narrow",
            "p1",
            List.of("p1"),
            List.of(new RoutineTrigger(RoutineTriggerKind.TIME, "0 7 * * *")),
            List.of(new RoutineCondition(RoutineConditionKind.TIME_WINDOW, "{\"start\":\"08:00\",\"end\":\"10:00\"}")),
            List.of(new RoutineAction(RoutineActionKind.DEVICE_STATE, "{\"device_id\":\"light-2\",\"power\":true}"))
        );
        RoutineCatalog cat = () -> List.of(narrow);
        new RoutineTickProcessor(clock, cat, reg, log).onTick();
        assertFalse(reg.get("light-2").orElseThrow().power());
    }
}
