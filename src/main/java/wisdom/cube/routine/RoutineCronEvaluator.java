package wisdom.cube.routine;

import java.time.ZonedDateTime;
import java.util.regex.Pattern;

/**
 * Minimal five-field cron match for {@link RoutineTriggerKind#TIME} payloads (F6.T4.S2).
 * Supports daily jobs: {@code M H * * *} with numeric minute/hour and {@code *} for day/month/dow.
 */
public final class RoutineCronEvaluator {

    private static final Pattern NUMERIC = Pattern.compile("^\\d+$");

    private RoutineCronEvaluator() {
    }

    /**
     * @param cronFiveField e.g. {@code 0 18 * * *}
     */
    public static boolean matches(String cronFiveField, ZonedDateTime zdt) {
        if (cronFiveField == null || cronFiveField.isBlank()) {
            return false;
        }
        String[] p = cronFiveField.trim().split("\\s+");
        if (p.length != 5) {
            return false;
        }
        if (!"*".equals(p[2]) || !"*".equals(p[3]) || !"*".equals(p[4])) {
            return false;
        }
        if (!numeric(p[0]) || !numeric(p[1])) {
            return false;
        }
        int minute = Integer.parseInt(p[0]);
        int hour = Integer.parseInt(p[1]);
        if (minute < 0 || minute > 59 || hour < 0 || hour > 23) {
            return false;
        }
        return zdt.getMinute() == minute && zdt.getHour() == hour;
    }

    private static boolean numeric(String s) {
        return NUMERIC.matcher(s).matches();
    }
}
