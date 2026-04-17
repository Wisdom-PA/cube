package wisdom.cube.routine;

import java.util.Objects;

/**
 * @param payload Integration-specific payload (e.g. JSON patch for a device, delay ms, message id).
 */
public record RoutineAction(RoutineActionKind kind, String payload) {

    public RoutineAction {
        Objects.requireNonNull(kind, "kind");
        payload = payload == null ? "" : payload;
    }
}
