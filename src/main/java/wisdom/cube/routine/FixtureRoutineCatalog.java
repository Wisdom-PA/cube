package wisdom.cube.routine;

import java.util.List;

/**
 * Dev fixture routines aligned with the previous static {@code /routines} payload (F6.T4.S1 scaffold).
 * Immutable; {@link #patchRoutineDisplayName} is not supported (use {@link MutableRoutineCatalog}).
 */
public final class FixtureRoutineCatalog implements RoutineCatalog {

    private final List<RoutineDefinition> definitions;

    public FixtureRoutineCatalog() {
        this(defaultDefinitions());
    }

    public FixtureRoutineCatalog(List<RoutineDefinition> definitions) {
        this.definitions = List.copyOf(definitions);
    }

    public static List<RoutineDefinition> defaultDefinitions() {
        return List.of(
            new RoutineDefinition(
                "r1",
                "Evening lights",
                "p1",
                List.of("p1", "p2"),
                List.of(new RoutineTrigger(RoutineTriggerKind.TIME, "0 18 * * *")),
                List.of(),
                List.of(new RoutineAction(
                    RoutineActionKind.DEVICE_STATE,
                    "{\"device_id\":\"light-1\",\"power\":true}"
                ))
            ),
            new RoutineDefinition(
                "r2",
                "Good morning",
                "p1",
                List.of("p1"),
                List.of(new RoutineTrigger(RoutineTriggerKind.TIME, "0 7 * * *")),
                List.of(new RoutineCondition(RoutineConditionKind.TIME_WINDOW, "{\"start\":\"06:00\",\"end\":\"10:00\"}")),
                List.of(
                    new RoutineAction(RoutineActionKind.DEVICE_STATE, "{\"device_id\":\"light-2\",\"power\":true}"),
                    new RoutineAction(RoutineActionKind.DELAY, "{\"ms\":300000}")
                )
            )
        );
    }

    @Override
    public List<RoutineDefinition> definitions() {
        return definitions;
    }
}
