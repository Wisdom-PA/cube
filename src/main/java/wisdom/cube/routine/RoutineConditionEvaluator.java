package wisdom.cube.routine;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates routine conditions on a tick (F6.T4.S2). {@link RoutineConditionKind#TIME_WINDOW} uses
 * {@code {"start":"HH:mm","end":"HH:mm"}}; other kinds are stubs that always pass until integrations land.
 */
public final class RoutineConditionEvaluator {

    private static final Pattern START = Pattern.compile(
        "\"start\"\\s*:\\s*\"((?:[01][0-9]|2[0-3]):[0-5][0-9])\"");
    private static final Pattern END = Pattern.compile(
        "\"end\"\\s*:\\s*\"((?:[01][0-9]|2[0-3]):[0-5][0-9])\"");

    private RoutineConditionEvaluator() {
    }

    public static boolean allSatisfied(RoutineDefinition def, ZonedDateTime zdt) {
        for (RoutineCondition c : def.conditions()) {
            if (!one(c, zdt)) {
                return false;
            }
        }
        return true;
    }

    private static boolean one(RoutineCondition c, ZonedDateTime zdt) {
        return switch (c.kind()) {
            case TIME_WINDOW -> timeWindow(c.payload(), zdt);
            case PRESENCE, DEVICE_STATE -> true;
        };
    }

    private static boolean timeWindow(String payload, ZonedDateTime zdt) {
        if (payload == null || payload.isBlank()) {
            return false;
        }
        Matcher ms = START.matcher(payload);
        Matcher me = END.matcher(payload);
        if (!ms.find() || !me.find()) {
            return false;
        }
        LocalTime start = LocalTime.parse(ms.group(1));
        LocalTime end = LocalTime.parse(me.group(1));
        LocalTime now = zdt.toLocalTime();
        if (!start.isAfter(end)) {
            return !now.isBefore(start) && !now.isAfter(end);
        }
        return now.compareTo(start) >= 0 || now.compareTo(end) <= 0;
    }
}
