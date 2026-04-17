package wisdom.cube.routine;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutineConditionEvaluatorTest {

    @Test
    void timeWindowInclusiveSameDay() {
        RoutineDefinition def = new RoutineDefinition(
            "x",
            "n",
            "p1",
            List.of(),
            List.of(),
            List.of(new RoutineCondition(RoutineConditionKind.TIME_WINDOW, "{\"start\":\"06:00\",\"end\":\"10:00\"}")),
            List.of()
        );
        ZonedDateTime ok = ZonedDateTime.of(2026, 4, 17, 7, 30, 0, 0, ZoneOffset.UTC);
        assertTrue(RoutineConditionEvaluator.allSatisfied(def, ok));
        ZonedDateTime early = ZonedDateTime.of(2026, 4, 17, 5, 0, 0, 0, ZoneOffset.UTC);
        assertFalse(RoutineConditionEvaluator.allSatisfied(def, early));
    }

    @Test
    void emptyConditionsPass() {
        RoutineDefinition def = new RoutineDefinition("x", "n", "p1", List.of(), List.of(), List.of(), List.of());
        assertTrue(RoutineConditionEvaluator.allSatisfied(def, ZonedDateTime.now(ZoneOffset.UTC)));
    }

    @Test
    void timeWindowRejectsMalformedPayload() {
        RoutineDefinition def = new RoutineDefinition(
            "x",
            "n",
            "p1",
            List.of(),
            List.of(),
            List.of(new RoutineCondition(RoutineConditionKind.TIME_WINDOW, "{}")),
            List.of()
        );
        assertFalse(RoutineConditionEvaluator.allSatisfied(def, ZonedDateTime.now(ZoneOffset.UTC)));
    }

    @Test
    void timeWindowOvernightSpan() {
        RoutineDefinition def = new RoutineDefinition(
            "x",
            "n",
            "p1",
            List.of(),
            List.of(),
            List.of(new RoutineCondition(RoutineConditionKind.TIME_WINDOW, "{\"start\":\"22:00\",\"end\":\"06:00\"}")),
            List.of()
        );
        ZonedDateTime late = ZonedDateTime.of(2026, 4, 17, 23, 0, 0, 0, ZoneOffset.UTC);
        assertTrue(RoutineConditionEvaluator.allSatisfied(def, late));
        ZonedDateTime early = ZonedDateTime.of(2026, 4, 17, 5, 0, 0, 0, ZoneOffset.UTC);
        assertTrue(RoutineConditionEvaluator.allSatisfied(def, early));
        ZonedDateTime noon = ZonedDateTime.of(2026, 4, 17, 12, 0, 0, 0, ZoneOffset.UTC);
        assertFalse(RoutineConditionEvaluator.allSatisfied(def, noon));
    }

    @Test
    void presenceAndDeviceStateConditionsPassStub() {
        RoutineDefinition def = new RoutineDefinition(
            "x",
            "n",
            "p1",
            List.of(),
            List.of(),
            List.of(
                new RoutineCondition(RoutineConditionKind.PRESENCE, ""),
                new RoutineCondition(RoutineConditionKind.DEVICE_STATE, "")
            ),
            List.of()
        );
        assertTrue(RoutineConditionEvaluator.allSatisfied(def, ZonedDateTime.now(ZoneOffset.UTC)));
    }
}
