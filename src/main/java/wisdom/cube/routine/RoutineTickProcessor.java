package wisdom.cube.routine;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;

import wisdom.cube.device.LightDeviceRegistry;
import wisdom.cube.logging.InMemoryBehaviourLogStore;

/**
 * One evaluation pass: for each routine whose time trigger matches {@code now} and conditions pass,
 * runs actions and logs the outcome (F6.T4.S2–S4).
 */
public final class RoutineTickProcessor {

    private final Clock clock;
    private final RoutineCatalog catalog;
    private final LightDeviceRegistry registry;
    private final InMemoryBehaviourLogStore behaviourLog;
    private final RoutineRunner runner = new RoutineRunner();

    public RoutineTickProcessor(
        Clock clock,
        RoutineCatalog catalog,
        LightDeviceRegistry registry,
        InMemoryBehaviourLogStore behaviourLog
    ) {
        this.clock = clock;
        this.catalog = catalog;
        this.registry = registry;
        this.behaviourLog = behaviourLog;
    }

    public void onTick() {
        ZonedDateTime zdt = ZonedDateTime.now(clock);
        for (RoutineDefinition def : catalog.definitions()) {
            if (!hasMatchingTimeTrigger(def, zdt)) {
                continue;
            }
            if (!RoutineConditionEvaluator.allSatisfied(def, zdt)) {
                continue;
            }
            List<RoutineStepResult> steps = runner.run(def, registry);
            behaviourLog.recordRoutineRun(def, steps);
        }
    }

    private static boolean hasMatchingTimeTrigger(RoutineDefinition def, ZonedDateTime zdt) {
        for (RoutineTrigger t : def.triggers()) {
            if (t.kind() == RoutineTriggerKind.TIME && RoutineCronEvaluator.matches(t.payload(), zdt)) {
                return true;
            }
        }
        return false;
    }
}
