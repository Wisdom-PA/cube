package wisdom.cube.routine;

import java.util.List;
import java.util.Objects;

/**
 * Named routine with ownership, edit permissions, triggers, optional conditions, and actions (F6.T4.S1).
 *
 * @param editorProfileIds Profiles allowed to edit this routine (household editors); empty means owner-only.
 */
public record RoutineDefinition(
    String routineId,
    String name,
    String ownerProfileId,
    List<String> editorProfileIds,
    List<RoutineTrigger> triggers,
    List<RoutineCondition> conditions,
    List<RoutineAction> actions
) {

    public RoutineDefinition {
        routineId = Objects.requireNonNull(routineId, "routineId");
        name = Objects.requireNonNull(name, "name");
        ownerProfileId = Objects.requireNonNull(ownerProfileId, "ownerProfileId");
        editorProfileIds = List.copyOf(editorProfileIds);
        triggers = List.copyOf(triggers);
        conditions = List.copyOf(conditions);
        actions = List.copyOf(actions);
    }
}
