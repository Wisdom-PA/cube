package wisdom.cube.routine;

import java.util.Objects;

/**
 * @param payload Integration-specific payload (e.g. cron expression, device id, phrase).
 */
public record RoutineTrigger(RoutineTriggerKind kind, String payload) {

    public RoutineTrigger {
        Objects.requireNonNull(kind, "kind");
        payload = payload == null ? "" : payload;
    }
}
