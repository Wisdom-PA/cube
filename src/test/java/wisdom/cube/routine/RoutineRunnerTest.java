package wisdom.cube.routine;

import java.util.List;

import org.junit.jupiter.api.Test;

import wisdom.cube.device.InMemoryLightDeviceRegistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutineRunnerTest {

    private final RoutineRunner runner = new RoutineRunner();

    @Test
    void deviceStatePatchAppliesPower() {
        InMemoryLightDeviceRegistry reg = new InMemoryLightDeviceRegistry();
        reg.setPower("light-1", false);
        RoutineDefinition def = new RoutineDefinition(
            "r",
            "n",
            "p1",
            List.of(),
            List.of(),
            List.of(),
            List.of(new RoutineAction(RoutineActionKind.DEVICE_STATE, "{\"device_id\":\"light-1\",\"power\":true}"))
        );
        List<RoutineStepResult> steps = runner.run(def, reg);
        assertEquals(1, steps.size());
        assertTrue(steps.get(0).success());
        assertTrue(reg.get("light-1").orElseThrow().power());
    }

    @Test
    void continuesAfterMissingDevice() {
        InMemoryLightDeviceRegistry reg = new InMemoryLightDeviceRegistry();
        RoutineDefinition def = new RoutineDefinition(
            "r",
            "n",
            "p1",
            List.of(),
            List.of(),
            List.of(),
            List.of(
                new RoutineAction(RoutineActionKind.DEVICE_STATE, "{\"device_id\":\"nope\",\"power\":true}"),
                new RoutineAction(RoutineActionKind.DEVICE_STATE, "{\"device_id\":\"light-2\",\"power\":true}")
            )
        );
        List<RoutineStepResult> steps = runner.run(def, reg);
        assertEquals(2, steps.size());
        assertFalse(steps.get(0).success());
        assertTrue(steps.get(1).success());
        assertTrue(reg.get("light-2").orElseThrow().power());
    }

    @Test
    void unreachableDeviceFailsStep() {
        InMemoryLightDeviceRegistry reg = new InMemoryLightDeviceRegistry();
        reg.setReachable("light-1", false);
        RoutineDefinition def = new RoutineDefinition(
            "r",
            "n",
            "p1",
            List.of(),
            List.of(),
            List.of(),
            List.of(new RoutineAction(RoutineActionKind.DEVICE_STATE, "{\"device_id\":\"light-1\",\"power\":true}"))
        );
        RoutineStepResult step = runner.run(def, reg).get(0);
        assertFalse(step.success());
        assertEquals("UNREACHABLE", step.errorCode());
    }

    @Test
    void deviceStateBrightnessOnly() {
        InMemoryLightDeviceRegistry reg = new InMemoryLightDeviceRegistry();
        RoutineDefinition def = new RoutineDefinition(
            "r",
            "n",
            "p1",
            List.of(),
            List.of(),
            List.of(),
            List.of(new RoutineAction(RoutineActionKind.DEVICE_STATE, "{\"device_id\":\"light-1\",\"brightness\":0.25}"))
        );
        assertTrue(runner.run(def, reg).get(0).success());
        assertEquals(0.25, reg.get("light-1").orElseThrow().brightness(), 1e-9);
    }

    @Test
    void delayAndNotificationStepsSucceed() {
        InMemoryLightDeviceRegistry reg = new InMemoryLightDeviceRegistry();
        RoutineDefinition def = new RoutineDefinition(
            "r",
            "n",
            "p1",
            List.of(),
            List.of(),
            List.of(),
            List.of(
                new RoutineAction(RoutineActionKind.DELAY, "{\"ms\":1}"),
                new RoutineAction(RoutineActionKind.NOTIFICATION, "{}")
            )
        );
        List<RoutineStepResult> steps = runner.run(def, reg);
        assertTrue(steps.get(0).success());
        assertTrue(steps.get(1).success());
    }
}
