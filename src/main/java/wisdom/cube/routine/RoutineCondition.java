package wisdom.cube.routine;

import java.util.Objects;

/**
 * @param payload Integration-specific payload (e.g. JSON window spec).
 */
public record RoutineCondition(RoutineConditionKind kind, String payload) {

    public RoutineCondition {
        Objects.requireNonNull(kind, "kind");
        payload = payload == null ? "" : payload;
    }
}
